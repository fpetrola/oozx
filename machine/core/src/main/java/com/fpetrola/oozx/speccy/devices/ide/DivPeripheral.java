/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the DivIDE and the DivMMC share: an 8K EPROM, some 8K pages of RAM, and the control
 * register at 0xe3 - bit 7 CONMEM forces the memory in, bit 6 MAPRAM puts RAM page 3 where the
 * EPROM was and can only be set until a hard reset, the low bits pick the page at 0x2000 - with
 * the automapper that pages the memory in when the processor reaches the ROM's entry points and
 * out again at 0x1ff8-0x1fff, but only while the EPROM is write-protected or MAPRAM is set.
 * Ported from Fuse's divxxx.c; unlike Fuse, the EPROM can be filled from a file at a hard reset.
 */
public abstract class DivPeripheral extends PluggablePeripheral implements ZxModule, RomcsDevice, IdeInterface {

  public static final int PAGE_SIZE = 0x2000;
  private static final int CONMEM = 0x80;
  private static final int MAPRAM = 0x40;
  private static final int[] ENTRIES = {0x0000, 0x0008, 0x0038, 0x0066, 0x04c6, 0x0562};

  protected final Memory memory;
  protected final Settings settings;
  protected final Cpu cpu;
  private final Module module;
  protected final MemoryPage[] eprom;
  private final MemoryPage[][] ram;
  private final List<PcTraps.Watch> watches = new ArrayList<>();

  protected SpectrumMachine on;
  private int control;
  private boolean active;
  private boolean automap;

  protected DivPeripheral(Memory memory, Module module, Cpu cpu, Settings settings, int ramPages) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    eprom = memory.newRam(PAGE_SIZE);
    ram = new MemoryPage[ramPages][];
    for (int i = 0; i < ramPages; i++) {
      ram[i] = memory.newRam(PAGE_SIZE);
      for (MemoryPage page : ram[i]) {
        page.setPageNum(i);
      }
    }
    Arrays.fill(eprom[0].getPage(), 0xff);
  }

  /** The file the EPROM is filled from at a hard reset, or null for one left erased. */
  public abstract String romName();

  /** Whether the EPROM's write-protect jumper is on. */
  public abstract boolean writeProtected();

  protected DefaultPortHandler controlPort() {
    return new DefaultPortHandler(0x00ff, 0x00e3, false, true) {
      public void write(int port, byte value) {
        controlWrite(value & 0xff);
      }
    };
  }

  /** A port write can set MAPRAM but never clear it. */
  public void controlWrite(int value) {
    control = value | control & MAPRAM;
    refresh();
  }

  public int control() {
    return control;
  }

  /** The automapper's own view, whether or not the jumpers let it act. */
  public void setAutomap(boolean automap) {
    this.automap = automap;
    refresh();
  }

  /** The jumpers changed: same registers, maybe another answer. */
  public void refresh() {
    if (on == null) {
      return;
    }
    if ((control & CONMEM) != 0) {
      page();
    } else if (writeProtected() || (control & MAPRAM) != 0) {
      if (automap) page(); else unpage();
    } else {
      unpage();
    }
  }

  private void page() {
    active = true;
    on.getRamInfo().romcs = true;
    on.memoryMap();
  }

  private void unpage() {
    active = false;
    on.getRamInfo().romcs = false;
    on.memoryMap();
  }

  @Override
  public void mapRom() {
    if (!active) {
      return;
    }
    int upper = control & ram.length - 1;
    MemoryPage[] lower;
    boolean lowerWritable, upperWritable;
    if ((control & CONMEM) != 0) {
      lower = eprom;
      lowerWritable = !writeProtected();
      upperWritable = true;
    } else if ((control & MAPRAM) != 0) {
      lower = ram[3];
      lowerWritable = false;
      upperWritable = upper != 3;
    } else {
      lower = eprom;
      lowerWritable = false;
      upperWritable = true;
    }
    for (MemoryPage page : lower) {
      page.setWritable(lowerWritable);
    }
    for (MemoryPage page : ram[upper]) {
      page.setWritable(upperWritable);
    }
    memory.mapRomcs8k(0x0000, lower);
    memory.mapRomcs8k(0x2000, ram[upper]);
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  /** The Sinclair machines with an edge connector that has /ROMCS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
    watches.add(cpu.beforeFetch().watch(0x3d00, 0x3dff, pc -> setAutomap(true)));
    watches.add(cpu.afterInstruction().watch(0x1ff8, 0x1fff, pc -> setAutomap(false)));
    for (int entry : ENTRIES) {
      watches.add(cpu.afterInstruction().watch(entry, pc -> setAutomap(true)));
    }
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    watches.forEach(PcTraps.Watch::off);
    watches.clear();
    if (on != null) {
      unpage();
    }
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    active = false;
    if (on == null) {
      return;
    }
    if (hard) {
      control = 0;
      for (MemoryPage[] page : ram) {
        Arrays.fill(page[0].getPage(), 0);
      }
      Arrays.fill(eprom[0].getPage(), 0xff);
      if (romName() != null) {
        try {
          memory.loadRomBank(eprom, 0, romName(), PAGE_SIZE, true);
        } catch (RomNotLoadedException missing) {
          Arrays.fill(eprom[0].getPage(), 0xff);
        }
      }
    } else {
      control &= MAPRAM;
    }
    automap = false;
    refresh();
  }

  @Override
  public boolean isPaged() {
    return active && on != null && on.getRamInfo().romcs;
  }

  @Override
  public String status() {
    return ((control & CONMEM) != 0 ? "CONMEM " : "") + ((control & MAPRAM) != 0 ? "MAPRAM " : "")
        + "page " + (control & ram.length - 1) + (writeProtected() ? ", EPROM protected" : ", EPROM writable");
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}

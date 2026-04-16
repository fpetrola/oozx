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

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
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
 * The EPROM can be filled from a file at a hard reset.
 */
public abstract class DivPeripheral extends PluggablePeripheral implements IdeInterface {

  public static final int PAGE_SIZE = 0x2000;
  private static final int CONMEM = 0x80;
  private static final int MAPRAM = 0x40;
  private static final int[] ENTRIES = {0x0000, 0x0008, 0x0038, 0x0066, 0x04c6, 0x0562};

  protected final MemoryBus memory;
  private boolean writeProtect;
  protected final Cpu cpu;
  protected final Ram eprom;
  private final Ram[] ram;
  private MappedMemory[] plugged = new MappedMemory[0];
  private final List<PcTraps.Watch> watches = new ArrayList<>();

  protected SpectrumMachine on;
  private int control;
  private boolean active;
  private boolean automap;

  protected DivPeripheral(MemoryBus memory, Cpu cpu, Roms roms, int ramPages) {
    super(List.of());
    this.roms = roms;
    this.memory = memory;
    this.cpu = cpu;
    eprom = new Ram(PAGE_SIZE);
    ram = new Ram[ramPages];
    for (int i = 0; i < ramPages; i++) {
      ram[i] = new Ram(PAGE_SIZE);
      ram[i].pageNum = i;
    }
    Arrays.fill(eprom.bytes, (byte) 0xff);
  }

  /** The file the EPROM is filled from at a hard reset, or null for one left erased. */
  private final Roms roms;

  /** With the jumper on, the machine can read the EPROM but not change it. */
  public boolean writeProtect() {
    return writeProtect;
  }

  public void setWriteProtect(boolean writeProtect) {
    this.writeProtect = writeProtect;
  }

  protected Wired controlPort() {
    return Wired.at(0x00ff, 0x00e3, new DefaultPortHandler(false, true) {
      public void write(int port, byte value) {
        controlWrite(value & 0xff);
      }
    });
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
    } else if (writeProtect() || (control & MAPRAM) != 0) {
      if (automap) page(); else unpage();
    } else {
      unpage();
    }
  }

  private void page() {
    active = true;
    protect();
    memory.unplug(plugged);
    plugged = new MappedMemory[]{new MappedMemory(0x0000, lower()), new MappedMemory(0x2000, upper())};
    memory.plug(plugged);
  }

  private void unpage() {
    active = false;
    memory.unplug(plugged);
    plugged = new MappedMemory[0];
  }

  private boolean mapram() {
    return (control & CONMEM) == 0 && (control & MAPRAM) != 0;
  }

  private Ram lower() {
    return mapram() ? ram[3] : eprom;
  }

  private Ram upper() {
    return ram[control & ram.length - 1];
  }

  /** What the control register lets be written: the EPROM only with CONMEM, and the RAM page 3 never while it stands in for it. */
  private void protect() {
    lower().writeProtected = (control & CONMEM) == 0 || writeProtect();
    upper().writeProtected = mapram() && upper() == ram[3];
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  /** The Sinclair machines with an edge connector that has /ROMCS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    watches.add(cpu.beforeFetch().watch(0x3d00, 0x3dff, pc -> setAutomap(true)));
    watches.add(cpu.afterInstruction().watch(0x1ff8, 0x1fff, pc -> setAutomap(false)));
    for (int entry : ENTRIES) {
      watches.add(cpu.afterInstruction().watch(entry, pc -> setAutomap(true)));
    }
  }

  @Override
  public void deactivate() {
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
      for (Ram page : ram) {
        Arrays.fill(page.bytes, (byte) 0);
      }
      Arrays.fill(eprom.bytes, (byte) 0xff);
      try {
        eprom.fill(roms.of(this, PAGE_SIZE));
      } catch (RomNotLoadedException missing) {
        // A Div with no EPROM image: the machine sees empty pages, as with the jumper off.
        Arrays.fill(eprom.bytes, (byte) 0xff);
      }
    } else {
      control &= MAPRAM;
    }
    automap = false;
    refresh();
  }

  @Override
  public boolean isPaged() {
    return active;
  }

  @Override
  public String status() {
    return ((control & CONMEM) != 0 ? "CONMEM " : "") + ((control & MAPRAM) != 0 ? "MAPRAM " : "")
        + "page " + (control & ram.length - 1) + (writeProtect() ? ", EPROM protected" : ", EPROM writable");
  }

}

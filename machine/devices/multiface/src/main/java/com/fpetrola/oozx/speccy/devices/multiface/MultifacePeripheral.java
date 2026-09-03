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
package com.fpetrola.oozx.speccy.devices.multiface;

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

import java.util.Arrays;
import java.util.List;

/**
 * A Multiface: a red button that stops whatever is running and takes it to the Multiface's own
 * ROM, which has 8K of RAM of its own next to it and can look at the whole machine from there.
 * <p>
 * The button pulls /NMI. The processor answers at 0x0066, and reaching that address is what pages
 * the ROM in - a flip-flop (IC8) latched by the button and released when the ROM has been entered.
 * Paging in and out afterwards is by reading a port, with A7 saying which; the 3 reads it the
 * other way round. J2 is the switch that lets the port page it in at all: on the One it is a
 * real switch, the stealth one; on the 128 and the 3 the ROM sets it by writing the port.
 */
public abstract class MultifacePeripheral extends PluggablePeripheral implements ZxModule, RomcsDevice {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;

  protected final MultifaceModel model;
  private final Memory memory;
  private final Module module;
  private final Cpu cpu;
  private final Settings settings;
  private final MemoryPage[] rom;
  private final MemoryPage[] ram;

  private SpectrumMachine on;
  private boolean available;
  private boolean paged;
  private boolean romcsBefore;
  private boolean activated;
  private boolean ic8aQ;
  private boolean ic8bQ;
  private boolean j2;
  private final int[] xfdd = new int[4];
  private PcTraps.Watch entering;

  protected MultifacePeripheral(MultifaceModel model, Memory memory, Module module, Cpu cpu, Settings settings) {
    super(List.of());
    this.model = model;
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    this.rom = memory.newRomBank();
    this.ram = memory.newRam(RAM_SIZE);
    DefaultPortHandler paging = new DefaultPortHandler(0x0072, model.portValue, true, true) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return (byte) portIn(port);
      }

      public void write(int port, byte value) {
        portOut(port);
      }
    };
    if (model == MultifaceModel.M3) {
      ports(paging, new DefaultPortHandler(0x90ff, 0x10fd, false, true) {
        public void write(int port, byte value) {
          xfdd[(port & 0x6000) >> 13] = value & 0x0f;
        }
      });
    } else {
      ports(paging);
    }
  }

  public MultifaceModel model() {
    return model;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return model.fitsOn(machine);
  }

  /** Its ROM and RAM go over the machine's, so it only arrives with a reset. */
  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    unpage();
    disarm();
    available = false;
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    unpage();
    disarm();
    available = false;
    if (on == null) {
      return;
    }
    if (hard) {
      Arrays.fill(ram[0].getPage(), (byte) 0);
    }
    ic8aQ = true;
    ic8bQ = true;
    j2 = model == MultifaceModel.ONE && !settings.current.multiface1Stealth;
    Arrays.fill(xfdd, 0);
    try {
      memory.loadRomBank(rom, 0, model.rom(settings), ROM_SIZE, !model.rom(settings).equals(model.defaultRom(settings)));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
  }

  /** Whether there is a ROM in it, which is what makes the button do anything. */
  public boolean isAvailable() {
    return available;
  }

  public boolean isPaged() {
    return paged;
  }

  /** The stealth switch on the One; on the others, whether the ROM has let the port page it in. */
  public boolean isJ2() {
    return j2;
  }

  public int ram(int address) {
    return ram[0].getPage()[address & (RAM_SIZE - 1)] & 0xff;
  }

  /**
   * The button. Pulls /NMI and arms the flip-flop that pages the ROM in as the processor
   * arrives at 0x0066, unless it was pressed already and the ROM has not been entered yet -
   * or, on a One switched to stealth, at all.
   */
  public boolean redButton() {
    if (!available || !ic8bQ || (model == MultifaceModel.ONE && !j2)) {
      return false;
    }
    ic8bQ = false;
    activated = true;
    entering = cpu.beforeFetch().watch(0x0066, pc -> enteredTheRom());
    cpu.nmi();
    return true;
  }

  private void enteredTheRom() {
    if (activated) {
      ic8aQ = false;
      activated = false;
      page();
    }
    disarm();
  }

  private void disarm() {
    if (entering != null) {
      entering.off();
      entering = null;
    }
    activated = false;
  }

  private int portIn(int port) {
    if (!available) {
      return 0xff;
    }
    boolean a7 = (port & 0x80) != 0;
    int answer = 0xff;
    switch (model) {
      case ONE -> {
        if (a7) {
          if (j2) {
            page();
            ic8aQ = false;
          }
        } else {
          unpage();
          ic8aQ = true;
        }
      }
      case M128 -> {
        if (a7) {
          if (j2) {
            page();
            // Which ROM the 128 has paged, for the SAVE routine to put things back.
            answer = (on.getRamInfo().lastByte & 0x08) != 0 ? 0xff : 0x7f;
            ic8aQ = false;
          }
        } else {
          unpage();
          ic8aQ = true;
        }
      }
      case M3 -> {
        if (a7) {
          unpage();
          ic8aQ = false;
        } else if (j2) {
          page();
          ic8aQ = true;
        }
        if (j2) {
          answer = xfdd[(port & 0x6000) >> 13] | 0xf0;
        }
      }
    }
    return answer;
  }

  private void portOut(int port) {
    if (!available) {
      return;
    }
    if (model != MultifaceModel.ONE && paged) {
      j2 = (port & 0x80) != 0;
    }
    ic8bQ = true;
  }

  private void page() {
    if (paged) {
      return;
    }
    paged = true;
    romcsBefore = on.getRamInfo().romcs;
    on.getRamInfo().romcs = true;
    on.memoryMap();
    if (model != MultifaceModel.ONE) {
      j2 = true;
    }
  }

  private void unpage() {
    if (!paged) {
      return;
    }
    paged = false;
    on.getRamInfo().romcs = romcsBefore;
    on.memoryMap();
  }

  @Override
  public void mapRom() {
    if (paged) {
      memory.mapRomcs8k(0x0000, rom);
      memory.mapRomcs8k(0x2000, ram);
    }
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}

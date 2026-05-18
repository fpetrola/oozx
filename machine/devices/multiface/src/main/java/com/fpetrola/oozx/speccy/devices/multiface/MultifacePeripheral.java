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

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonMerge;

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
public abstract class MultifacePeripheral extends PluggablePeripheral {
  private final Roms roms;

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;

  protected final MultifaceModel model;
  private final MemoryBus memory;
  private final Cpu cpu;
  private boolean stealth;
  private final com.fpetrola.oozx.speccy.modules.memory.Rom rom;
  private final Ram ram;
  private final MappedMemory[] held;

  private SpectrumMachine on;
  private boolean available;
  private boolean paged;
  private boolean activated;
  private boolean ic8aQ;
  private boolean ic8bQ;
  private boolean j2;
  private final int[] xfdd = new int[4];
  private PcTraps.Watch entering;

  protected MultifacePeripheral(MultifaceModel model, MemoryBus memory, Cpu cpu, Roms roms) {
    super(List.of());
    this.roms = roms;
    this.model = model;
    this.memory = memory;
    this.cpu = cpu;
    this.rom = new com.fpetrola.oozx.speccy.modules.memory.Rom(ROM_SIZE);
    this.ram = new Ram(RAM_SIZE);
    held = new MappedMemory[]{new MappedMemory(0x0000, rom), new MappedMemory(0x2000, ram)};
    Wired paging = Wired.at(0x0072, model.portValue, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(portIn(port));
      }

      public void write(int port, byte value) {
        portOut(port);
      }
    });
    if (model == MultifaceModel.M3) {
      ports(paging, Wired.at(0x90ff, 0x10fd, new DefaultPortHandler(false, true) {
        public void write(int port, byte value) {
          xfdd[(port & 0x6000) >> 13] = value & 0x0f;
        }
      }));
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
  }

  @Override
  public void deactivate() {
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
      Arrays.fill(ram.bytes, (byte) 0);
    }
    ic8aQ = true;
    ic8bQ = true;
    j2 = model == MultifaceModel.ONE && !stealth;
    Arrays.fill(xfdd, 0);
    try {
      rom.fill(roms.of(this, ROM_SIZE));
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
    return ram.bytes[address & (RAM_SIZE - 1)] & 0xff;
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
            answer = on.paging().screen() == 7 ? 0xff : 0x7f;
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
    memory.plug(held);
    if (model != MultifaceModel.ONE) {
      j2 = true;
    }
  }

  private void unpage() {
    if (!paged) {
      return;
    }
    paged = false;
    memory.unplug(held);
  }

  /** A Multiface One can be made invisible to the program it is about to interrupt. */
  public boolean stealth() {
    return stealth;
  }

  public void setStealth(boolean stealth) {
    this.stealth = stealth;
  }
}

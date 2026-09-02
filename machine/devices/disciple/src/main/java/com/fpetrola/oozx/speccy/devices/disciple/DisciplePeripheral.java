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
package com.fpetrola.oozx.speccy.devices.disciple;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.devices.plusd.MgtDiskInterface;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The DISCiPLE: the +D's WD1770, 8K of ROM and 8K of RAM, with the ROM and RAM halves able to
 * swap places (port 0x7b), a joystick port that Fuse reads only for the printer's BUSY, and a
 * network port Fuse does not emulate. It comes up paged in at a reset, and its ROM hooks
 * 0x0001, 0x0008, 0x0066 and 0x028e. Ported from Fuse's disciple.c.
 */
@Singleton
public class DisciplePeripheral extends MgtDiskInterface {

  private static final int[] HOOKS = {0x0001, 0x0008, 0x0066, 0x028e};

  private boolean memswap;
  private boolean inhibited;

  @Inject
  public DisciplePeripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                            Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, module, cpu, settings, events, machine, printer);
    ports(fdcRegister(0x1b, FdcRegister.STATUS_COMMAND),
        fdcRegister(0x5b, FdcRegister.TRACK),
        fdcRegister(0x9b, FdcRegister.SECTOR),
        fdcRegister(0xdb, FdcRegister.DATA),
        new DefaultPortHandler(0x00ff, 0x001f, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) (printerAttached() ? 0xff : 0xbf);
          }

          public void write(int port, byte value) {
            control(value & 0xff);
          }
        },
        new DefaultPortHandler(0x00ff, 0x003b, false, true) {
          public void write(int port, byte value) {
            // The network: not emulated, as in Fuse.
          }
        },
        new DefaultPortHandler(0x00ff, 0x007b, true, true) {
          public byte read(int port, byte[] attached) {
            swap(false);
            return 0;
          }

          public void write(int port, byte value) {
            swap(true);
          }
        },
        patchPort(0xbb),
        printerDataPort(0xfb));
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }

  @Override
  public String romName() {
    return settings.current.romDisciple;
  }

  @Override
  protected String defaultRomName() {
    return settings.defaults.romDisciple;
  }

  /** Comes up paged in, unlike the +D. */
  @Override
  protected boolean pagedAtReset() {
    return true;
  }

  @Override
  protected void reset(boolean hard) {
    memswap = false;
    inhibited = false;
  }

  private void swap(boolean swapped) {
    memswap = swapped;
    if (on != null) {
      on.memoryMap();
    }
  }

  public boolean isSwapped() {
    return memswap;
  }

  public boolean isInhibited() {
    return inhibited;
  }

  @Override
  protected void mapPages() {
    if (memswap) {
      memory.mapRomcs8k(0x0000, ram);
      memory.mapRomcs8k(0x2000, rom);
    } else {
      memory.mapRomcs8k(0x0000, rom);
      memory.mapRomcs8k(0x2000, ram);
    }
  }

  /** Bit 0 low is the second drive, bit 1 the side, bit 6 the printer's strobe, bit 4 the inhibit button. */
  @Override
  protected void control(int b) {
    controlRegister = b;
    selectDrive((b & 0x01) != 0 ? 0 : 1, (b & 0x02) != 0 ? 1 : 0);
    strobe((b & 0x40) != 0);
    if (on != null) {
      on.memoryMap();
    }
    if ((b & 0x10) != 0) {
      inhibited = true;
    }
  }

  /** Sold for the 48K. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
  }
}

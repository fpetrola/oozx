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

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.devices.plusd.MgtDiskInterface;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.machine.Roms;

/**
 * The DISCiPLE: the +D's WD1770, 8K of ROM and 8K of RAM, with the ROM and RAM halves able to
 * swap places (port 0x7b), a joystick port read only for the printer's BUSY, and a
 * network port that is not emulated. It comes up paged in at a reset, and its ROM hooks
 * 0x0001, 0x0008, 0x0066 and 0x028e.
 */
@Singleton
public class DisciplePeripheral extends MgtDiskInterface {

  private static final int[] HOOKS = {0x0001, 0x0008, 0x0066, 0x028e};

  private boolean memswap;
  private boolean inhibited;

  @Inject
  public DisciplePeripheral(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events,
                            Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, cpu, floppy, roms, events, machine, printer);
    ports(fdcRegister(0x1b, FdcRegister.STATUS_COMMAND),
        fdcRegister(0x5b, FdcRegister.TRACK),
        fdcRegister(0x9b, FdcRegister.SECTOR),
        fdcRegister(0xdb, FdcRegister.DATA),
        Wired.at(0x00ff, 0x001f, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            return BusAnswer.of((printerAttached() ? 0xff : 0xbf));
          }

          public void write(int port, byte value) {
            control(value & 0xff);
          }
        }),
        Wired.at(0x00ff, 0x003b, new DefaultPortHandler(false, true) {
          public void write(int port, byte value) {
            // The network: not emulated.
          }
        }),
        Wired.at(0x00ff, 0x007b, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            swap(false);
            // Zero, and not driven, which is what this has always put on the bus.
            return new BusAnswer(0, false);
          }

          public void write(int port, byte value) {
            swap(true);
          }
        }),
        patchPort(0xbb),
        printerDataPort(0xfb));
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
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
    if (isPaged()) page();
  }

  public boolean isSwapped() {
    return memswap;
  }

  public boolean isInhibited() {
    return inhibited;
  }

  @Override
  protected MappedMemory[] ranges() {
    return new MappedMemory[]{new MappedMemory(0x0000, memswap ? ram : rom), new MappedMemory(0x2000, memswap ? rom : ram)};
  }

  /** Bit 0 low is the second drive, bit 1 the side, bit 6 the printer's strobe, bit 4 the inhibit button. */
  @Override
  protected void control(int b) {
    controlRegister = b;
    selectDrive((b & 0x01) != 0 ? 0 : 1, (b & 0x02) != 0 ? 1 : 0);
    strobe((b & 0x40) != 0);
    if (isPaged()) page();
    if ((b & 0x10) != 0) {
      inhibited = true;
    }
  }

  /** Sold for the 48K. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
  }

  /** The DISCiPLE, a disk and network interface with its own ROM. */
}

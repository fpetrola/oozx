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
package com.fpetrola.oozx.speccy.devices.plusd;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.machine.Roms;

/**
 * The +D: the WD1770 on ports 0xe3/0xeb/0xf3/0xfb, paged by the ROM's hooks - the error restart,
 * the NMI, and the keyboard scan the ROM calls every interrupt - and by port 0xe7.
 */
@Singleton
public class PlusDPeripheral extends MgtDiskInterface {

  private static final int[] HOOKS = {0x0008, 0x003a, 0x0066, 0x028e};

  @Inject
  public PlusDPeripheral(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events,
                         Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, cpu, floppy, roms, events, machine, printer);
    ports(fdcRegister(0xe3, FdcRegister.STATUS_COMMAND),
        fdcRegister(0xeb, FdcRegister.TRACK),
        fdcRegister(0xf3, FdcRegister.SECTOR),
        fdcRegister(0xfb, FdcRegister.DATA),
        Wired.at(0x00ff, 0x00ef, new DefaultPortHandler(false, true) {
          public void write(int port, byte value) {
            control(value & 0xff);
          }
        }),
        patchPort(0xe7),
        Wired.at(0x00ff, 0x00f7, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            return BusAnswer.of((printerAttached() ? 0x7f : 0xff));
          }

          public void write(int port, byte value) {
            printerDataPort(0xf7).handler().write(port, value);
          }
        }));
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }


  /** Left active but not paged after a reset: the first hook pages it in. */
  @Override
  protected boolean pagedAtReset() {
    return false;
  }

  @Override
  protected void reset(boolean hard) {
  }

  @Override
  protected MappedMemory[] ranges() {
    return new MappedMemory[]{new MappedMemory(0x0000, rom), new MappedMemory(0x2000, ram)};
  }

  /** Bits 0-1 which drive (only 2 is the second), bit 7 the side, bit 6 the printer's strobe. */
  @Override
  protected void control(int b) {
    controlRegister = b;
    selectDrive((b & 0x03) == 2 ? 1 : 0, (b & 0x80) != 0 ? 1 : 0);
    strobe((b & 0x40) != 0);
  }

  /** The Sinclair machines with an edge connector that has /ROMCS: not a +2A or +3, not a clone. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  /** The +D, a disk interface with its own ROM. */
}

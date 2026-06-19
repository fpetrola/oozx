/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

/** +D: WD1770 on ports 0xe3/0xeb/0xf3/0xfb, paged by ROM hooks (error restart, NMI, the
 * per-interrupt keyboard scan) and by port 0xe7. */
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


  /** After reset, stays active but unpaged until the first ROM hook pages it in. */
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

  /** Control register: bits 0-1 select drive (value 2 = second drive), bit 7 side, bit 6 printer strobe. */
  @Override
  protected void control(int b) {
    controlRegister = b;
    selectDrive((b & 0x03) == 2 ? 1 : 0, (b & 0x80) != 0 ? 1 : 0);
    strobe((b & 0x40) != 0);
  }

  /** Sinclair models with a /ROMCS edge connector: excludes +2A/+3 and clones. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

}

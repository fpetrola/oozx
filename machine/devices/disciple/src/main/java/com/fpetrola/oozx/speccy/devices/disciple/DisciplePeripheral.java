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
 * DISCiPLE: same WD1770/8K-ROM/8K-RAM design as the +D, with ROM/RAM halves swappable via port
 * 0x7b, a joystick port doubling as printer BUSY, and an unemulated network port. Starts paged
 * in after reset; ROM hooks at 0x0001, 0x0008, 0x0066, 0x028e.
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
            // Network hardware is not modelled.
          }
        }),
        Wired.at(0x00ff, 0x007b, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            swap(false);
            // Always returns 0; this port has never been driven with real data.
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


  /** Unlike the +D, this board starts paged in immediately. */
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

  /** Control register: bit 0 low selects drive 2, bit 1 side, bit 6 printer strobe, bit 4 inhibit. */
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

  /** Sold for machines with neither 128-style paging nor full port decoding (the 48K). */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
  }
}

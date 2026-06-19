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
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.WdDiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.machine.Roms;

/** Common MGT interface shape beyond the controller: 8K ROM, 8K RAM, a "patch" port (read
 * pages in, write pages out), a control register (drive/side/printer strobe), and a printer port. */
public abstract class MgtDiskInterface extends WdDiskInterface {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;
  public static final int DRIVES = 2;

  private final ParallelPrinterPeripheral printer;
  protected int controlRegister;

  protected MgtDiskInterface(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events,
                             Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, cpu, floppy, roms, events, machine, WdFdc.Type.WD1770, WdFdc.FLAG_NONE, DRIVES, ROM_SIZE, RAM_SIZE);
    this.printer = printer;
  }

  /** Writes the control register: drive select, side, printer strobe, plus board-specific bits. */
  protected abstract void control(int b);

  /** The patch port: reading pages this interface in, writing pages it out. */
  protected Wired patchPort(int port) {
    return Wired.at(0x00ff, port, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        page();
        // Always returns 0; this line has never carried real data.
        return new BusAnswer(0, false);
      }

      public void write(int port, byte value) {
        unpage();
      }
    });
  }

  /** The port that carries outgoing printer data. */
  protected Wired printerDataPort(int port) {
    return Wired.at(0x00ff, port, new DefaultPortHandler(false, true) {
      public void write(int port, byte value) {
        printer.printer().write(value);
      }
    });
  }

  protected boolean printerAttached() {
    return printer.isWanted();
  }

  protected void strobe(boolean on) {
    printer.printer().strobe(on);
  }

  public int controlRegister() {
    return controlRegister;
  }

  @Override
  protected Disk blank() throws DiskException {
    return Disk.blank(2, 80, Disk.Density.DD, Disk.Type.MGT);
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"mgt", "img", "dsk"};
  }
}

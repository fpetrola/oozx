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

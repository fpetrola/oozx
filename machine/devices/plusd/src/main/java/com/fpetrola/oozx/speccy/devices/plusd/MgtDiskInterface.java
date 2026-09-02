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

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.WdDiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

/**
 * What Miles Gordon Technology's two interfaces have in common beyond the controller: 8K of ROM
 * and 8K of RAM, a "patch" port whose read pages them in and whose write pages them out, a
 * control register with the drive, the side and the printer's strobe in it, and the printer port.
 */
public abstract class MgtDiskInterface extends WdDiskInterface {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;
  public static final int DRIVES = 2;

  private final ParallelPrinterPeripheral printer;
  protected int controlRegister;

  protected MgtDiskInterface(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                             Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, module, cpu, settings, events, machine, WdFdc.Type.WD1770, WdFdc.FLAG_NONE, DRIVES, ROM_SIZE, RAM_SIZE);
    this.printer = printer;
  }

  /** The control register: drive, side, the printer's strobe, and whatever else the board has. */
  protected abstract void control(int b);

  /** The port whose read pages the interface in and whose write pages it out. */
  protected DefaultPortHandler patchPort(int port) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        page();
        return 0;
      }

      public void write(int port, byte value) {
        unpage();
      }
    };
  }

  /** The port the printer's data goes out on. */
  protected DefaultPortHandler printerDataPort(int port) {
    return new DefaultPortHandler(0x00ff, port, false, true) {
      public void write(int port, byte value) {
        printer.printer().write(value);
      }
    };
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

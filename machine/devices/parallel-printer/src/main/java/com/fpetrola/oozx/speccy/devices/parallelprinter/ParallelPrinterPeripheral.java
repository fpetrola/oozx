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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.List;

/**
 * The printer as a machine sees it. A +2A or +3 has the port on its back: bit 0 reads BUSY,
 * low for a printer that is never busy, and a write latches the data; the strobe is bit 4 of the
 * +3's own paging port, which the machine hands over. A +D or a DISCiPLE brings its own port and
 * hands the printer the bytes itself, so on those machines this has no ports and is only asked
 * whether it is there.
 */
@com.google.inject.Singleton
public class ParallelPrinterPeripheral extends PluggablePeripheral {

  private final ParallelPrinter printer;

  @com.google.inject.Inject
  public ParallelPrinterPeripheral(ParallelPrinter printer) {
    super(List.of());
    this.printer = printer;
    ports(Wired.at(0xf002, 0x0000, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(0xfe);
      }

      public void write(int port, byte value) {
        printer.write(value);
      }
    }), Wired.at(0xf002, 0x1000, new DefaultPortHandler(false, true) {
      /** The strobe is bit 4 of the +3's own paging port, next to the disk motor. */
      public void write(int port, byte value) {
        printer.strobe((value & 0x10) != 0);
      }
    }));
  }

  public ParallelPrinter printer() {
    return printer;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.pagesThrough1ffd();
  }
}

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

package com.fpetrola.oozx.speccy.devices.printer;

import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

/**
 * The one port a ZX Printer has, at two decodings: a Sinclair machine only checks that bit 2 is
 * low, a Timex checks the whole byte for 0xFB.
 */
class ZxPrinterPortHandler extends DefaultPortHandler {
  private final ZxPrinter printer;

  ZxPrinterPortHandler(ZxPrinter printer) {
    super(true, true);
    this.printer = printer;
  }

  public BusAnswer read(int port) {
    return BusAnswer.of(printer.read());
  }

  public void write(int port, byte value) {
    printer.write(value);
  }
}

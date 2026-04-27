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

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.util.List;

@com.google.inject.Singleton
public class ZxPrinterPeripheral extends PluggablePeripheral {
  private final ZxPrinter printer;

  @com.google.inject.Inject
  public ZxPrinterPeripheral(ZxPrinter printer) {
    this(0x0004, 0x0000, printer);
  }

  ZxPrinterPeripheral(int mask, int value, ZxPrinter printer) {
    super(List.of(Wired.at(mask, value, new ZxPrinterPortHandler(printer))));
    this.printer = printer;
  }

  public Printout paper() {
    return printer.paper();
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
  }
}

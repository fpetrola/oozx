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

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * The printer, and the two ways a machine's port bus reaches it. One belt and one roll of paper
 * behind both: which decoding is switched on depends on the machine, but there is only ever one
 * printer on the desk.
 */
public class PrinterDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(ZxPrinterPeripheral.class);
    devices.addBinding().to(ZxPrinterFullDecodePeripheral.class);
  }

  /**
   * Ticks that never go backwards, out of a clock that is rebased every frame and a machine that
   * counts them. Whichever machine is running is the one being asked.
   */
  @Provides
  @Singleton
  ZxPrinter printer(Printout paper, Z80Clock clock, Machine machine) {
    return new ZxPrinter(paper,
        () -> machine.current.frameCount() * machine.current.getTimings().tstatesPerFrame() + clock.getTStates(),
        () -> machine.current.getTimings().tstatesPerFrame());
  }

  @Provides
  @Singleton
  Printout paper() {
    return new Printout();
  }

}

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

package com.fpetrola.oozx.speccy.devices.interface1;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** The Interface 1: eight Microdrives, an RS232 and the ZX Net behind one shadow ROM. */
public class Interface1Devices extends AbstractModule implements Extension {
  protected void configure() {
    com.fpetrola.oozx.config.Settings.mirror(binder(), "interface1", Interface1Peripheral.class, "microdriveSectors", "randomMicrodriveLength", "rs232Handshake", "rawNetwork");
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(Interface1Peripheral.class);
  }
}

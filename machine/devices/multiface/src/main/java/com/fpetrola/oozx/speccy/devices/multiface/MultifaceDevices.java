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
package com.fpetrola.oozx.speccy.devices.multiface;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** The three Multifaces, each for the machine it was sold for. */
public class MultifaceDevices extends AbstractModule implements Extension {
  protected void configure() {
    com.fpetrola.oozx.config.Settings.mirror(binder(), "multiface", MultifaceOnePeripheral.class, "stealth");
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(MultifaceOnePeripheral.class);
    devices.addBinding().to(Multiface128Peripheral.class);
    devices.addBinding().to(Multiface3Peripheral.class);
  }
}

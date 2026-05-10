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

package com.fpetrola.oozx.speccy.devices.joystick;

import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.modules.input.Input;

/** The Kempston interface. */
public class JoystickDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(KempstonStrictPeripheral.class);
  }

  @Provides
  @Singleton
  KempstonStrictPeripheral strict(Joystick joystick, Input.Setup input) {
    return new KempstonStrictPeripheral(joystick, () -> input.kempstonJoystick);
  }

}

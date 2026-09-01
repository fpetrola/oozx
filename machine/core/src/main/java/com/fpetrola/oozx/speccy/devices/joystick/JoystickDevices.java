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

package com.fpetrola.oozx.speccy.devices.joystick;

import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/** The Kempston interface, in its two decodings. */
public class JoystickDevices extends AbstractModule {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(KempstonStrictPeripheral.class);
    devices.addBinding().to(KempstonLoosePeriphPeripheral.class);
  }

  @Provides
  @Singleton
  KempstonStrictPeripheral strict(Joystick joystick, Settings settings) {
    return new KempstonStrictPeripheral(joystick, () -> settings.current.joyKempston);
  }

  @Provides
  @Singleton
  KempstonLoosePeriphPeripheral loose(Joystick joystick, Settings settings) {
    return new KempstonLoosePeriphPeripheral(joystick, () -> settings.current.joyKempston);
  }
}

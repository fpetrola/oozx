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

package com.fpetrola.oozx.speccy.devices.ay;

import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/** The sound chip a machine has, the one a +3 wires differently, and the box for a machine with none. */
public class AyDevices extends AbstractModule {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(AyPeripheral.class);
    devices.addBinding().to(AyPlus3Peripheral.class);
    devices.addBinding().to(MelodikPeripheral.class);
  }

  /** The box answers from the settings flag, and this is where that flag is turned into a question. */
  @Provides
  @Singleton
  MelodikPeripheral melodik(Sound sound, Z80Clock clock, Settings settings) {
    return new MelodikPeripheral(sound, clock, () -> settings.current.melodik);
  }
}

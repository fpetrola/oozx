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

package com.fpetrola.oozx.speccy.devices.melodik;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The AY box for a machine that has none of its own, which is a thing somebody bought and plugged
 * in - so it ships with the peripherals and not with the emulator, unlike the chip a 128K has on
 * its board.
 */
public class MelodikDevices extends AbstractModule implements Extension {
  protected void configure() {
    com.fpetrola.oozx.config.Settings.mirror(binder(), "melodik", MelodikPeripheral.class, "fitted");
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(MelodikPeripheral.class);
  }

}

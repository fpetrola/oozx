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
package com.fpetrola.oozx.speccy.devices.beta128;

import com.fpetrola.oozx.speccy.devices.DeviceModule;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** The Beta 128 somebody plugged into a 48K or a 128 - the board a Pentagon has on its own. */
public class Beta128Devices extends AbstractModule implements DeviceModule {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(PluggedBeta128Peripheral.class);
  }
}

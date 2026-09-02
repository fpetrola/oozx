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

package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.fpetrola.oozx.speccy.devices.DeviceModule;

/** The floppy controller a +3 has. */
public class DiskDevices extends AbstractModule implements DeviceModule {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(Upd765Peripheral.class);
    devices.addBinding().to(Beta128Peripheral.class);
  }
}

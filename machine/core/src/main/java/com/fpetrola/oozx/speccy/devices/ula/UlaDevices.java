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

package com.fpetrola.oozx.speccy.devices.ula;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** The ULA as a machine reaches it: loosely decoded, or on every bit of its port. */
public class UlaDevices extends AbstractModule {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(UlaPeripheral.class);
    devices.addBinding().to(UlaFullDecodePeripheral.class);
  }
}

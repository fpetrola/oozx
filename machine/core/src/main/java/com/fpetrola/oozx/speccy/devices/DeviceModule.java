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

package com.fpetrola.oozx.speccy.devices;

import com.google.inject.Module;

/**
 * A package of devices, found rather than named: the emulator loads every one of these declared on
 * the classpath and installs it, so a device that arrives in a jar nobody compiled against can
 * still say what it brings and which machines it fits.
 * <p>
 * It is a Guice module because discovery answers who is out there and injection answers what each
 * one needs, and a device needs the sound, the clock or a machine's module to work.
 */
public interface DeviceModule extends Module {
}

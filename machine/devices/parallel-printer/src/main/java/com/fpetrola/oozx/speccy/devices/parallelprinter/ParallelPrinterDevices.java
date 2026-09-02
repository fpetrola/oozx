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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.speccy.devices.DeviceModule;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ParallelPrinterDevices extends AbstractModule implements DeviceModule {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(ParallelPrinterPeripheral.class);
  }

  /** Ticks that never go backwards, out of a clock that is rebased every frame. */
  @Provides
  @Singleton
  ParallelPrinter printer(Z80Clock clock, Machine machine) {
    return new ParallelPrinter(
        () -> machine.current.frameCount() * machine.current.getTimings().tstatesPerFrame + clock.getTStates());
  }
}

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

package com.fpetrola.oozx.speccy.devices.printer;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.speccy.devices.DeviceModule;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * The printer, and the two ways a machine's port bus reaches it. One belt and one roll of paper
 * behind both: which decoding is switched on depends on the machine, but there is only ever one
 * printer on the desk.
 */
public class PrinterDevices extends AbstractModule implements DeviceModule {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(ZxPrinterPeripheral.class);
    devices.addBinding().to(ZxPrinterFullDecodePeripheral.class);
  }

  /**
   * Ticks that never go backwards, out of a clock that is rebased every frame and a machine that
   * counts them. Whichever machine is running is the one being asked.
   */
  @Provides
  @Singleton
  ZxPrinter printer(Printout paper, Z80Clock clock, Machine machine) {
    return new ZxPrinter(paper,
        () -> machine.current.frameCount() * machine.current.getTimings().tstatesPerFrame + clock.getTStates(),
        () -> machine.current.getTimings().tstatesPerFrame);
  }

  @Provides
  @Singleton
  Printout paper() {
    return new Printout();
  }

}

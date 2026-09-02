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

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.util.List;

@com.google.inject.Singleton
public class ZxPrinterPeripheral extends PluggablePeripheral {
  private final ZxPrinter printer;

  @com.google.inject.Inject
  public ZxPrinterPeripheral(ZxPrinter printer) {
    this(0x0004, 0x0000, printer);
  }

  ZxPrinterPeripheral(int mask, int value, ZxPrinter printer) {
    super(List.of(new ZxPrinterPortHandler(mask, value, printer)));
    this.printer = printer;
  }

  public Printout paper() {
    return printer.paper();
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
  }
}

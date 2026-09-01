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

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;


/** The same printer on a machine that reads the whole port: 0xFB and nothing else. */
@com.google.inject.Singleton
public class ZxPrinterFullDecodePeripheral extends ZxPrinterPeripheral {
  @com.google.inject.Inject
  public ZxPrinterFullDecodePeripheral(ZxPrinter printer) {
    super(0x00ff, 0x00fb, printer);
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.fullyDecodesPorts() && !machine.has(com.fpetrola.oozx.MachineCapability.MEMORY_128);
  }
}

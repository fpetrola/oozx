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
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A ZX Printer plugged into a Sinclair machine, which decodes its port on one bit.
 * <p>
 * A 128K is offered no such thing: its COPY talks to a serial printer through the AY instead, so a
 * 128 printing nothing on this port is the machine behaving correctly. The +D shares this port,
 * and when one of those arrives the two must not both be switched on.
 */
public class ZxPrinterPeripheral extends AbstractPeripheral {
  private final ZxPrinter printer;
  private final BooleanSupplier wanted;

  public ZxPrinterPeripheral(ZxPrinter printer, BooleanSupplier wanted) {
    this(0x0004, 0x0000, printer, wanted);
  }

  ZxPrinterPeripheral(int mask, int value, ZxPrinter printer, BooleanSupplier wanted) {
    super(List.of(new ZxPrinterPortHandler(mask, value, printer)));
    this.printer = printer;
    this.wanted = wanted;
  }

  public Printout paper() {
    return printer.paper();
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean isWanted() {
    return wanted.getAsBoolean();
  }
}

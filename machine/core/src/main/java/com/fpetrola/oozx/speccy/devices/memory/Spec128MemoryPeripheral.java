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

package com.fpetrola.oozx.speccy.devices.memory;


import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;

import com.fpetrola.oozx.speccy.machine.Spec128;

import java.util.List;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.speccy.machine.Paging128;
import com.google.inject.Inject;

@com.google.inject.Singleton
public class Spec128MemoryPeripheral extends AbstractPeripheral {
  private Paging128 machine;

  @Inject
  public Spec128MemoryPeripheral() {
    super(List.of());
    ports(new Spec128PortHandler(0x8002, 0x0000, () -> machine));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    this.machine = (Paging128) machine;
  }

  /** The 128's pager, on the machines whose paging is only that - the +3 pages differently. */
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.has(MachineCapability.MEMORY_128) && !machine.has(MachineCapability.PLUS3_MEMORY);
  }
}

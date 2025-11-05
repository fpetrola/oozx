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


import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;


import java.util.List;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.machine.PagingPlus3;
import com.google.inject.Inject;

@com.google.inject.Singleton
public class SpecPlus3MemoryPeripheral extends AbstractPeripheral {
  private PagingPlus3 machine;

  @Inject
  public SpecPlus3MemoryPeripheral() {
    super(List.of());
    ports(Wired.at(0xc002, 0x4000, new Spec128PortHandler(() -> machine)),
        Wired.at(0xf002, 0x1000, new SpecPlus3PortHandler(() -> machine)));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    this.machine = (PagingPlus3) machine;
  }

}

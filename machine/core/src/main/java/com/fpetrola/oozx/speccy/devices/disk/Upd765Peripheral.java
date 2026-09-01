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


import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;


import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

import java.util.List;
import com.fpetrola.oozx.speccy.machine.FloppyDrive;
import com.google.inject.Inject;

@com.google.inject.Singleton
public class Upd765Peripheral extends AbstractPeripheral {
  private FloppyDrive machine;

  @Inject
  public Upd765Peripheral() {
    super(List.of());
    ports(new FdcPortHandler(0xf002, 0x3000, () -> machine),
        new FdcStatusPortHandler(0xf002, 0x2000, () -> machine));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    this.machine = (FloppyDrive) machine;
  }

  /** The drive is the +3's; a +2A is the same machine without one. */
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.has(MachineCapability.PLUS3_DISK);
  }
}

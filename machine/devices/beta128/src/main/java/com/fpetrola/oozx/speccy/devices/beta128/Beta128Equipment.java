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
package com.fpetrola.oozx.speccy.devices.beta128;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.DriveBayFrame;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.DiskInterface;

public class Beta128Equipment implements Equipment {
  static final DiskInterface SHAPE = DiskInterface.shape(4, "Boot",
      "Reset the machine into TR-DOS, with the 48 BASIC underneath, and boot from drive A", "trd", "scl");

  public String name() {
    return "Beta 128";
  }

  /** On a Pentagon the bay is the machine's own Beta; on anything else it is the one plugged in. */
  public DeviceFrame<?> open() {
    return new DriveBayFrame<Beta128Peripheral>("Beta 128", PluggedBeta128Peripheral.class, SHAPE) {
      @Override
      protected Beta128Peripheral find(Speccy machine) {
        return (Beta128Peripheral) machine.peripherals.find(machine.machine.current.has(MachineCapability.TRDOS_DISK)
            ? Beta128Peripheral.class : PluggedBeta128Peripheral.class);
      }
    };
  }
}

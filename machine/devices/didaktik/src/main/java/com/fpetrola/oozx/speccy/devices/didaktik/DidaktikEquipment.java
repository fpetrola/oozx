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
package com.fpetrola.oozx.speccy.devices.didaktik;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.DriveBayFrame;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.disk.DiskInterface;

public class DidaktikEquipment implements Equipment {
  static final DiskInterface SHAPE = DiskInterface.shape(2, "SNAP",
      "The SNAP button: an NMI that the Didaktik's ROM takes over, to save what is running", "d80", "d40");

  public String name() {
    return "Didaktik 40/80";
  }

  public DeviceFrame<?> open() {
    return new DriveBayFrame<>("Didaktik 40/80", DidaktikPeripheral.class, SHAPE);
  }
}

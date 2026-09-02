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

package com.fpetrola.oozx.speccy.devices.plusd;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.devices.DriveBayFrame;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.disk.DiskInterface;

public class PlusDEquipment implements Equipment {
  /** Two drives, MGT images, an NMI button: what a +D is before a machine has one. */
  static final DiskInterface SHAPE = DiskInterface.shape(2, "NMI",
      "The button on the +D: stops the program and brings up its snapshot menu", "mgt", "img", "dsk");

  public String name() {
    return "+D";
  }

  public DeviceFrame<?> open() {
    return new DriveBayFrame<>("+D", PlusDPeripheral.class, SHAPE);
  }
}

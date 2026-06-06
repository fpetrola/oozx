/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

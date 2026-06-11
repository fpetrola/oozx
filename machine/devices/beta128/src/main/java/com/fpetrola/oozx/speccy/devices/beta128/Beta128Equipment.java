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
package com.fpetrola.oozx.speccy.devices.beta128;

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
        return (Beta128Peripheral) machine.peripheralRegistry.find(machine.machine.current.hasOnBoard(Beta128Peripheral.class)
            ? Beta128Peripheral.class : PluggedBeta128Peripheral.class);
      }
    };
  }
}

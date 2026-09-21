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

package com.fpetrola.oozx.speccy.tools.cassette;

import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.modules.tape.Tape;

import java.io.File;

/** The cassette deck, offered like any other equipment: found, not named by the application. */
public class CassetteEquipment implements Equipment {
  public String name() {
    return "Cassette";
  }

  public MachineFrame open() {
    return new CassetteFrame();
  }

  /** Which files are cassettes is the deck's own business, so the deck is asked. */
  public boolean opens(File file) {
    return Tape.isATape(file.getName());
  }
}

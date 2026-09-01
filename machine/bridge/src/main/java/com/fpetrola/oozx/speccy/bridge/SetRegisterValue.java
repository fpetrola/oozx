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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.speccy.bridge.LibretroCore;

public class SetRegisterValue implements EmulatorCommand<Object> {
  public final String name;
  public final int value;

  public SetRegisterValue(String name, int value) {
    this.name = name;
    this.value = value;
  }

  public String toString() {
    return "SetRegisterValue{" +
            "name='" + name + '\'' +
            ", value=" + String.format("%02X", value) +
            '}';
  }

  public Object execute(LibretroCore core) {
    core.retro_set_register_data(name, value);
    return null;
  }
}

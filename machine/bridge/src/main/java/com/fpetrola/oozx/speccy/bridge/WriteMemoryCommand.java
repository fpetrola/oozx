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
import com.sun.jna.Structure;

import java.util.List;

// WriteMemoryCommand
public class WriteMemoryCommand extends Structure implements EmulatorCommand<Object> {
  public int address;
  public int value;
  public boolean contended;

  public WriteMemoryCommand(int address, int value, boolean contended) {
    this.address = address;
    this.value = value;
    this.contended = contended;
  }

  @Override
  protected List<String> getFieldOrder() {
    return List.of("address", "value");
  }

  public String toString() {
    return "WriteMemoryCommand{" +
            "address=" + String.format("%04X", address) +
            ", value=" + String.format("%02X", value) +
            '}';
  }

  public Object execute(LibretroCore core) {
    if (contended) {
      core.retro_set_memory_data_contended(address, value);
    } else
      core.retro_set_memory_data(address, value);

    return null;
  }
}

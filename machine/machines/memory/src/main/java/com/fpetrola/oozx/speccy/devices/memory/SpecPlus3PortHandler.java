/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.devices.memory;

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.machine.PagingPlus3;

class SpecPlus3PortHandler extends DefaultPortHandler {
  private final Supplier<PagingPlus3> machine;

  public SpecPlus3PortHandler(Supplier<PagingPlus3> machine) {
    super(false, true);
    this.machine = machine;
  }

  public void write(int port, byte value) {
    machine.get().memoryPort2Write(port, value);
  }
}

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
package com.fpetrola.oozx.speccy.devices.multiface;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MultifaceOnePeripheral extends MultifacePeripheral {
  @Inject
  public MultifaceOnePeripheral(MemoryBus memory, Cpu cpu, com.fpetrola.oozx.speccy.machine.Roms roms) {
    super(MultifaceModel.ONE, memory, cpu, roms);
  }
}

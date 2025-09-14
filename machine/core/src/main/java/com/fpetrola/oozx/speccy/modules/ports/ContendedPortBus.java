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
package com.fpetrola.oozx.speccy.modules.ports;

import com.fpetrola.oozx.speccy.ports.PortBus;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** The bus as the ULA times it: an access waits for the video before and after. */
@Singleton
public class ContendedPortBus implements PortBus {
  private final Ula ula;
  private final Z80Clock z80Clock;
  private final MachinePortBus peripheralBus;

  @Inject
  public ContendedPortBus(Ula ula, Z80Clock z80Clock, MachinePortBus peripheralBus) {
    this.ula = ula;
    this.z80Clock = z80Clock;
    this.peripheralBus = peripheralBus;
  }

  public void write(int port, byte b) {
    ula.contendPortEarly(port);
    peripheralBus.writeInternal(port, b);
    ula.contendPortLate(port);
    z80Clock.addTStates(1);
  }

  public byte read(int port) {
    ula.contendPortEarly(port);
    ula.contendPortLate(port);
    return peripheralBus.read(port);
  }
}

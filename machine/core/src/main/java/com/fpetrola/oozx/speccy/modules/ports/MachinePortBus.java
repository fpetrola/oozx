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
package com.fpetrola.oozx.speccy.modules.ports;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.google.inject.Provider;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.ports.Backplane;
import com.fpetrola.oozx.speccy.ports.DecodedPortBus;
import com.fpetrola.oozx.speccy.ports.PortBus;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The machine's side of the port bus: it knows which machine is running, which says what a port
 * nobody drove reads as, and that an access costs a T state.
 */
@Singleton
public class MachinePortBus implements PortBus {
  private final Z80Clock z80Clock;
  private final DecodedPortBus bus;
  private final Supplier<Machine> machine;

  @Inject
  public MachinePortBus(Z80Clock z80Clock, Backplane backplane, Provider<Machine> machine) {
    this.z80Clock = z80Clock;
    this.machine = Suppliers.memoize(machine::get);
    this.bus = new DecodedPortBus(backplane, port -> (byte) this.machine.get().current.unattachedPort(port));
  }

  public byte read(int port) {
    byte value = bus.read(port);
    z80Clock.addTStates(1);
    return value;
  }

  public void write(int port, byte b) {
    writeInternal(port, b);
    z80Clock.addTStates(1);
  }

  /** A write that takes no time: the ULA-timed bus puts it between its two waits, and the bridge puts back what the reference wrote. */
  public void writeInternal(int port, byte b) {
    bus.write(port, b);
  }
}

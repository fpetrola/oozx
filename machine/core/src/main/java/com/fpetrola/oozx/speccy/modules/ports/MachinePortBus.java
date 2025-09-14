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

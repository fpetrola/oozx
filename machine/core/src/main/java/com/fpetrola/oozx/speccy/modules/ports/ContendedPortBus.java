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

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

package com.fpetrola.oozx;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.Ula;
import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.fpetrola.z80.cpu.Z80Clock;

@Singleton
public class ContendedPeripheralBus implements PeripheralBusDelegate {
  private final Ula ula;
  private final Z80Clock z80Clock;
  private final PeripheralBus peripherals;

@Inject
  public ContendedPeripheralBus(Ula ula, Z80Clock z80Clock, PeripheralBus peripherals) {
    this.ula = ula;
    this.z80Clock = z80Clock;
    this.peripherals = peripherals;
  }

  public PeripheralBus getPeripherals() {
    return peripherals;
  }

  public void writePort(int port, byte b) {
    ula.contendPortEarly(port);
    writePortInternal(port, b);
    ula.contendPortLate(port);
    z80Clock.addTStates(1);
  }

  public byte readPort(int port) {
    ula.contendPortEarly(port);
    ula.contendPortLate(port);
    return getPeripherals().readPort(port);
  }

  public void machineChanged(SpectrumMachine newMachine) {
    peripherals.machineChanged(newMachine);
  }
}

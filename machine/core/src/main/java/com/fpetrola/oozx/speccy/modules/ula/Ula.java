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

package com.fpetrola.oozx.speccy.modules.ula;

import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.google.inject.Provider;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryContention;
import com.fpetrola.z80.tstates.Contention.Kind;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;


@Singleton
public class Ula implements MemoryContention {
  private final MemoryBus memory;

  /** How long an access is held up at each T-state of a frame: worked out once, for the machine that is on. */
  public final ContentionTable contention = new ContentionTable();

  private final SpectrumZ80Clock z80Clock;
  private final Supplier<Machine> machine;

@Inject
  public Ula(MemoryBus memory, SpectrumZ80Clock z80Clock, Provider<Machine> machine) {
    this.machine = Suppliers.memoize(machine::get);
    this.memory = memory;
    memory.contention = this;
    this.z80Clock = z80Clock;
  }

  private SpectrumMachine getCurrent() {
    return machine.get().current;
  }

  public void contendPortEarly(int port) {
    if (memory.contended(port)) {
      addUlaStates(Kind.PORT_EARLY, 0);
    }
    z80Clock.contend(Kind.PORT_EARLY, 1);
  }

  public void contendPortLate(int port) {
    if (getCurrent().portFromUla(port)) {
      addUlaStates(Kind.PORT_LATE, 2);
    } else {
      if (memory.contended(port)) {
        addUlaStates(Kind.PORT_LATE, 1);
        addUlaStates(Kind.PORT_LATE, 1);
        addUlaStates(Kind.PORT_LATE, 0);
      } else {
        z80Clock.contend(Kind.PORT_LATE, 2);
      }
    }
  }

  /** What the ULA takes from a read of a contended page while it is drawing. */
  public void beforeRead() {
    z80Clock.contend(Kind.READ, contention.delay[ContentionTable.within(z80Clock.getTStates())]);
  }

  public void beforeWrite() {
    z80Clock.contend(Kind.WRITE, contention.delay[ContentionTable.within(z80Clock.getTStates())]);
  }

  public void addUlaStates(Kind cycle, int states) {
    z80Clock.contend(cycle, contention.delayNoMreq[ContentionTable.within(z80Clock.getTStates())] + states);
  }
}


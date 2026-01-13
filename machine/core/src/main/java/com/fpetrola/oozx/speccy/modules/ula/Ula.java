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


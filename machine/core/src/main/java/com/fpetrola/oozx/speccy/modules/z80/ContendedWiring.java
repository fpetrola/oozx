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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.function.Function;

/**
 * The wiring a Spectrum has: every access goes through the memory the ULA shares, which is what
 * holds the processor up, and the phases of a cycle are counted as they happen.
 */
@Singleton
public class ContendedWiring implements ProcessorWiring {
  private final MemoryBus memory;
  private final Ula ula;
  private final Display display;
  private final SpectrumZ80Clock clock;

  @Inject
  public ContendedWiring(MemoryBus memory, Ula ula, Display display, SpectrumZ80Clock clock) {
    this.memory = memory;
    this.ula = ula;
    this.display = display;
    this.clock = clock;
  }

  @Override
  public OOZ80 build(Core core, Function<Memory, State> states) {
    ContendedMemory contended = new ContendedMemory(memory, clock, !core.countsItsOwnContention());
    State state = states.apply(core.wrapping(contended));
    SpeccyPhaseProcessor phases = new SpeccyPhaseProcessor(state, memory, ula, clock);
    contended.watchedBy(phases);
    return core.cpu(state, phases);
  }
}

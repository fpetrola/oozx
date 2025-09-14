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
    State state = states.apply(contended);
    SpeccyPhaseProcessor phases = new SpeccyPhaseProcessor(state, memory, ula, clock);
    contended.watchedBy(phases);
    return core.cpu(state, phases);
  }
}

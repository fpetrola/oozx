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

package model.harness;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.z80.ProcessorWiring;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.AbstractMemory;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.fpetrola.z80.tstates.AddStatesMemoryReadListener;
import com.fpetrola.z80.tstates.AddStatesMemoryWriteListener;

import java.util.function.Function;

/**
 * The wiring a test uses when it wants to count T-states itself: the processor reaches memory
 * without the clock and without the contention, and every access is told to the machine's bus
 * afterwards, which is what adds the T-states it would have cost.
 * <p>
 * Not in the model, which knows only {@link com.fpetrola.oozx.speccy.modules.z80.ContendedWiring}:
 * a machine built for a person has no use for this one, and the model used to carry both and pick
 * between them by asking a static boolean who was looking.
 */
@Singleton
public class CountingWiring implements ProcessorWiring {

  /** Binds this over the machine's own, for whoever builds a Speccy that counts its own T-states. */
  public static Module instead() {
    return binder -> OptionalBinder.newOptionalBinder(binder, ProcessorWiring.class)
        .setBinding().to(CountingWiring.class);
  }

  private final MemoryBus memory;
  private final Ula ula;
  private final SpectrumZ80Clock clock;

  @Inject
  public CountingWiring(MemoryBus memory, Ula ula, SpectrumZ80Clock clock) {
    this.memory = memory;
    this.ula = ula;
    this.clock = clock;
  }

  /** A core that reads the memory tables itself never reaches the listeners, so its count would be short. */
  @Override
  public boolean takesACoreThatReachesMemoryItself() {
    return false;
  }

  @Override
  public OOZ80 build(Core core, Function<Memory, State> states) {
    Memory plain = new AbstractMemory() {
      protected int doRead(final int address) {
        return memory.peek(address);
      }

      protected void doWrite(final int address, final int value) {
        memory.poke(address, (byte) value);
      }

      public void reset() {
      }
    };
    State state = states.apply(plain);
    CountedPhases phases = new CountedPhases(state, memory, ula, clock);
    OOZ80 processor = core.cpu(state, phases);
    plain.addMemoryReadListener(new AddStatesMemoryReadListener(phases) {
      protected void doRead(int address, int value, int fetching) {
        memory.read(address);
      }
    });
    plain.addMemoryWriteListener(new AddStatesMemoryWriteListener(phases) {
      protected void doWrite(int address, int value) {
        memory.write(address, (byte) (value & 0xff));
      }
    });
    return processor;
  }
}

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

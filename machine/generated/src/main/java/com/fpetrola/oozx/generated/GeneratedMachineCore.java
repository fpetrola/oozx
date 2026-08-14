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
package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.generate.GeneratedCore;
import com.fpetrola.z80.generate.GeneratedZ80Cpu;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.tstates.PhaseProcessor;

import java.util.List;

/**
 * The core generated against this machine, as a Core: an instance of the generated class over the
 * machine's objects, which it holds, and over the pages it reaches its memory through without a
 * call. The memory the Z80 builds stays the state's, for whoever asks the state.
 */
public class GeneratedMachineCore implements Core {
  static final String NAME = "Generated";
  /**
   * Asked for only when this core is the one to run: making it means generating and compiling,
   * which a machine that starts on another processor should not pay for.
   */
  private final java.util.function.Supplier<Class<?>> type;
  private final MemoryBus memory;
  private final Ula ula;
  private final SpectrumZ80Clock clock;
  private final Display display;
  private GeneratedCore generated;

  GeneratedMachineCore(java.util.function.Supplier<Class<?>> type, MemoryBus memory, Ula ula, SpectrumZ80Clock clock, Display display) {
    this.type = type;
    this.memory = memory;
    this.ula = ula;
    this.clock = clock;
    this.display = display;
  }

  public String name() {
    return NAME;
  }

  public RegisterBank bank(com.fpetrola.z80.memory.Memory unused, IO io) {
    List<GeneratedCores.Held> held = GeneratedCores.held(memory, ula, clock, display, io);
    try {
      generated = (GeneratedCore) type.get().getConstructor(held.stream().map(GeneratedCores.Held::type).toArray(Class[]::new))
          .newInstance(held.stream().map(GeneratedCores.Held::value).toArray());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("the generated core cannot be built over this machine", e);
    }
    return (RegisterBank) generated;
  }

  public OOZ80 cpu(State state, PhaseProcessor contention) {
    return new GeneratedZ80Cpu(state, generated);
  }

  public boolean countsItsOwnContention() {
    return true;
  }
}

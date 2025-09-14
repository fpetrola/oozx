/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;

import java.util.function.Function;

/**
 * How a processor is wired to the machine it runs on: what it reaches memory through, and what
 * counts the T-states of every cycle it spends there.
 * <p>
 * A machine has one and does not choose it. The emulator binds the one it runs on and whoever
 * builds a machine for something else binds another, which is why nothing here names an
 * alternative: this used to be a static boolean saying whether a person or a test was looking,
 * and every machine in the process answered the same, whichever had asked.
 */
public interface ProcessorWiring {

  /**
   * The processor of that core, over this machine.
   *
   * @param states makes the processor's state over whichever memory this wiring puts it behind
   */
  OOZ80 build(Core core, Function<Memory, State> states);

  /**
   * Whether a core that reads the machine's memory tables itself, instead of through the memory
   * this wiring builds, still counts its T-states right. One that does not is not offered.
   */
  default boolean takesACoreThatReachesMemoryItself() {
    return true;
  }
}

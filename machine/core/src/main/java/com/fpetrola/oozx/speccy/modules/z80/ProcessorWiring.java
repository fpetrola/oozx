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

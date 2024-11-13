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
package com.fpetrola.z80.cpu;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.tstates.PhaseProcessor;

/**
 * What a processor runs on: the register bank its state is built over, and the processor over that
 * state. The OOP core takes the default bank and is told its contention by the aspect; a generated
 * core is its own bank and carries the contention inside.
 */
public interface Core {
  /** What this implementation of the processor is called, where someone has to choose one. */
  String name();

  RegisterBank bank(Memory memory, IO io);

  /** The processor over the state, reporting its contention to {@code contention}. */
  OOZ80 cpu(State state, PhaseProcessor contention);

  /** When the core counts its contention itself, the memory it runs on must not tell the aspect about its accesses. */
  boolean countsItsOwnContention();
}

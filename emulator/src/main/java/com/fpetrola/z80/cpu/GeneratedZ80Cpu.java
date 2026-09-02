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

import com.fpetrola.z80.instructions.types.Instruction;

/**
 * The generated core as a Z80Cpu: OOZ80 keeps the reset, the interrupts and their timing, and
 * one instruction step is the generated switch instead of a fetch and an execute.
 */
public class GeneratedZ80Cpu extends OOZ80 {
  private final GeneratedZ80 core;

  public GeneratedZ80Cpu(State state, GeneratedZ80 core) {
    super(state, new InstructionFetcher() {
      public Instruction fetchNextInstruction() {
        return null;
      }

      public void reset() {
      }
    }, new InstructionExecutor() {
      public Instruction getInstructionAt(int address) {
        return null;
      }

      public Instruction execute(Instruction instruction) {
        return instruction;
      }

      public boolean isExecuting(Instruction instruction) {
        return false;
      }
    });
    this.core = core;
    core.attach(state);
  }

  public Instruction execute(int cycles) {
    core.step();
    return null;
  }

  public GeneratedZ80 getCore() {
    return core;
  }
}

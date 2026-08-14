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
package com.fpetrola.z80.generate;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.types.Instruction;

/**
 * The generated core as a Z80Cpu: OOZ80 keeps the reset, the interrupts and their timing, and
 * one instruction step is the generated switch instead of a fetch and an execute.
 */
public class GeneratedZ80Cpu extends OOZ80 {
  private final GeneratedCore core;

  public GeneratedZ80Cpu(State state, GeneratedCore core) {
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

  public GeneratedCore getCore() {
    return core;
  }
}

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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.PC;

public class InstructionFetcherForTest implements InstructionFetcher {
  protected List<Instruction> instructions = new ArrayList<>();
  private int i;
  protected Register pc;
  protected final State state;
  protected final InstructionExecutor instructionExecutor;

  public InstructionFetcherForTest(State state, InstructionExecutor instructionExecutor) {
    pc = state.getRegister(PC);
    this.state = state;
    this.instructionExecutor = instructionExecutor;
  }

  public Instruction fetchNextInstruction() {
    // Solo busca, igual que DefaultInstructionFetcher: ejecutar y avanzar el PC
    // es responsabilidad del InstructionExecutor que OOZ80 invoca despues.
    return instructions.get(pc.read());
  }

  protected void updatePC(Instruction instruction) {
    int nextPC = -1;
    if (instruction instanceof AbstractInstruction jumpInstruction)
      nextPC = jumpInstruction.getNextPC();

    if (nextPC == -1) {
      nextPC = (pc.read() + 1) & 0xFFFF;
    }

    pc.write(nextPC);
  }

  public void reset() {
    pc.write(0);
    instructions.clear();
    instructionExecutor.reset();
  }

  public int add(Instruction instruction) {
    instructions.add(instruction);
    return instructions.size();
  }

  public Instruction getInstructionAt(int i) {
    return instructions.get(i);
  }

  public Instruction getTransformedInstructionAt(int i) {
    return instructions.get(i);
  }
}

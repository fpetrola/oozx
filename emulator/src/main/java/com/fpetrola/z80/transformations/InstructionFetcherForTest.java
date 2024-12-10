/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.List;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.PC;

public class InstructionFetcherForTest<T extends WordNumber> implements InstructionFetcher {
  protected List<Instruction<T>> instructions = new ArrayList<>();
  private int i;
  protected Register<T> pc;
  protected final State<T> state;
  protected final InstructionExecutor instructionExecutor;

  public InstructionFetcherForTest(State<T> state, InstructionExecutor instructionExecutor) {
    pc = state.getRegister(PC);
    this.state = state;
    this.instructionExecutor = instructionExecutor;
  }

  public void fetchNextInstruction() {
    Instruction<T> instruction = instructions.get(pc.read().intValue());
    Instruction execute = instructionExecutor.execute(instruction);
    System.out.println(execute);
    updatePC(instruction);
  }

  protected void updatePC(Instruction<T> instruction) {
    T nextPC = null;
    if (instruction instanceof AbstractInstruction jumpInstruction)
      nextPC = (T) jumpInstruction.getNextPC();

    if (nextPC == null)
      nextPC = pc.read().plus(1);

    pc.write(nextPC);
  }

  public void reset() {
    pc.write(createValue(0));
    instructions.clear();
  }

  public int add(Instruction<T> instruction) {
    instructions.add(instruction);
    return instructions.size();
  }

  public Instruction<T> getInstructionAt(int i) {
    return instructions.get(i);
  }

  public Instruction getTransformedInstructionAt(int i) {
    return instructions.get(i);
  }
}

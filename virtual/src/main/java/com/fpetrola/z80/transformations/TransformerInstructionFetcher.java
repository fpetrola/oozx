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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class TransformerInstructionFetcher<T extends WordNumber> extends InstructionFetcherForTest<T> {
  private final TransformerInstructionExecutor<T> instructionExecutor1;

  public TransformerInstructionFetcher(State<T> state, TransformerInstructionExecutor instructionExecutor) {
    super(state);
    instructionExecutor1 = instructionExecutor;
  }

  public Instruction<T> fetchNextInstruction() {
    updatePC(instructionExecutor1.execute(instructions.get(pc.read().intValue())));
    return null;
  }

  protected void updatePC(Instruction<T> instruction) {
    T nextPC = null;
    if (instruction instanceof AbstractInstruction jumpInstruction)
      nextPC = (T) jumpInstruction.getNextPC();

    if (nextPC == null)
      nextPC = pc.read().plus(instruction.getLength());

    pc.write(nextPC);
  }

  public Instruction<T> getTransformedInstructionAt(int i) {
    return instructionExecutor1.getInstructionAt(i);
  }
}

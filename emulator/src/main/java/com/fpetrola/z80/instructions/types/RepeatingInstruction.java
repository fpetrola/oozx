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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.RegisterPair;

public class RepeatingInstruction extends AbstractInstruction implements JumpInstruction {
  final protected BlockInstruction instructionToRepeat;
  final private ImmutableOpcodeReference pc;
  final protected RegisterPair bc;

  public RepeatingInstruction(BlockInstruction instructionToRepeat, ImmutableOpcodeReference pc, RegisterPair bc) {
    this.instructionToRepeat = instructionToRepeat;
    this.pc = pc;
    this.bc = bc;
  }

  public void execute() {
    instructionToRepeat.execute();
    setNextPC(checkLoopCondition() ? pc.read() : -1);
  }

  protected boolean checkLoopCondition() {
    return bc.getHigh().read() != 0;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitRepeatingInstruction(this))
      super.accept(visitor);
  }

  public BlockInstruction getInstructionToRepeat() {
    return instructionToRepeat;
  }

  public ImmutableOpcodeReference getPc() {
    return pc;
  }

  public RegisterPair getBc() {
    return bc;
  }
}

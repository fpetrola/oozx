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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class LdOperation<T extends WordNumber> extends AbstractInstruction<T> {
  protected Instruction<T> instruction;
  protected OpcodeReference<T> target;

  public LdOperation(OpcodeReference target, Instruction<T> instruction) {
    this.target = target;
    this.instruction = instruction;
    incrementLengthBy(1);
  }

  public int execute() {
    instruction.execute();
    if (instruction instanceof TargetInstruction<T> targetInstruction) {
      T read;
      if (targetInstruction.getTarget() instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
        read = memoryPlusRegister8BitReference.value;
      } else
        read = targetInstruction.getTarget().read();
      target.write(read);
    }
    return cyclesCost;
  }

  public String toString() {
    return "LD " + target + "," + instruction;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    instruction.accept(visitor);
    target.accept(visitor);
    if (!visitor.visitLdOperation(this)) {
      super.accept(visitor);
    }
  }
}

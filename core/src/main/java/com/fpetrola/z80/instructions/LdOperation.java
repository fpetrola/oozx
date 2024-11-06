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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.instructions.base.AbstractInstruction;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.TargetInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class LdOperation<T extends WordNumber> extends AbstractInstruction<T> {
  protected Instruction<T> instruction;
  protected OpcodeReference<T> target;

  public LdOperation(OpcodeReference target, Instruction<T> instruction) {
    this.target = target;
    this.instruction = instruction;
  }

  public int execute() {
    instruction.execute();
    if (instruction instanceof TargetInstruction<T> targetInstruction) {
      T read = targetInstruction.getTarget().read();
      target.write(read);
    }
    return cyclesCost;
  }

  public String toString() {
    return "LD " + target + "," + instruction;
  }
}

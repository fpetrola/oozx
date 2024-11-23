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

import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class JR<T extends WordNumber> extends ConditionalInstruction<T, Condition> {

  public JR(ImmutableOpcodeReference target, Condition condition, Register<T> pc) {
    super(target, condition, pc);
  }

  public T calculateJumpAddress() {
    return calculateRelativeJumpAddress();
  }

  protected int jumpIfConditionMatches() {
    if (condition.conditionMet(this)) {
      T jumpAddress2 = calculateJumpAddress();
      jumpAddress2 = beforeJump(jumpAddress2);
      setJumpAddress(jumpAddress2);
      setNextPC(jumpAddress2);
    } else
      setNextPC(null);

    return cyclesCost;
  }


  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingJR(this);
  }
}

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

import com.fpetrola.z80.instructions.base.ConditionalInstruction;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.BNotZeroCondition;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class DJNZ<T extends WordNumber> extends ConditionalInstruction<T, BNotZeroCondition<T>> {
  public DJNZ(ImmutableOpcodeReference<T> target, BNotZeroCondition condition, Register<T> pc) {
    super(target, null, pc);
    this.condition = condition;
  }

  public int execute() {
    condition.getB().decrement();
    return super.execute();
  }

  public T calculateJumpAddress() {
    return calculateRelativeJumpAddress();
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingDjnz(this);
  }

  public String toString() {
    return getClass().getSimpleName() + " " + positionOpcodeReference;
  }
}

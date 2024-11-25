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
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Ret<T extends WordNumber> extends ConditionalInstruction<T, Condition> {
  private final Memory<T> memory;
  private final Register<T> sp;

  public Ret(Condition condition, Register<T> sp, Memory<T> memory, Register<T> pc) {
    super(sp, condition, pc);
    this.memory = memory;
    this.sp = sp;
  }

  protected T beforeJump(T jumpAddress) {
    return Pop.doPop(memory, sp);
  }

  public String toString() {
    String conditionStr = condition.toString();
    return "RET" + ((conditionStr.length() > 0) ? " " + conditionStr : "");
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingRet(this))
      super.accept(visitor);
  }
}

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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Call<T extends WordNumber> extends ConditionalInstruction<T, Condition> {
  private final Register<T> sp;
  protected final Memory<T> memory;

  public Call(ImmutableOpcodeReference positionOpcodeReference, Condition condition, Register<T> pc, Register<T> sp, Memory<T> memory) {
    super(positionOpcodeReference, condition, pc);
    this.sp = sp;
    this.memory = memory;
  }

  public T beforeJump(T jumpAddress) {
    T value = pc.read().plus(length);
    Push.doPush(value, sp, memory);
    return jumpAddress;
  }

  @Override
  public int execute() {
    T jumpAddress2 = calculateJumpAddress();
    if (condition.conditionMet(this)) {
      jumpAddress2 = beforeJump(jumpAddress2);
      setJumpAddress(jumpAddress2);
      setNextPC(jumpAddress2);
    } else
      setNextPC(null);

    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingCall(this))
      super.accept(visitor);
  }
}

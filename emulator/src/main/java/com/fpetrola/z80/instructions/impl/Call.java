/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
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
    return super.execute();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingCall(this))
      super.accept(visitor);
  }
}

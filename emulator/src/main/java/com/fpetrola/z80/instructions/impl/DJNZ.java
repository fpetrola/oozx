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

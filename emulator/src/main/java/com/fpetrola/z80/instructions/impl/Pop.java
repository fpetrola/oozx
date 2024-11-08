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

import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Pop<T extends WordNumber> extends DefaultTargetFlagInstruction<T> {
  protected final Register<T> sp;
  protected final Memory<T> memory;

  public Pop(OpcodeReference target, Register<T> sp, Memory<T> memory, Register<T> flag) {
    super(target, flag);
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    T value = doPop(memory, sp);
    target.write(value);
    return 5 + 3 + 3;
  }

  public static <T extends WordNumber> T doPop(Memory<T> memory, Register<T> sp) {
    memory.disableReadListener();
    final T value = Memory.read16Bits(memory, sp.read());
    sp.increment();
    sp.increment();
    memory.enableReadListener();
//    System.out.println("pop: " + value);
    return value;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingPop(this);
  }
}

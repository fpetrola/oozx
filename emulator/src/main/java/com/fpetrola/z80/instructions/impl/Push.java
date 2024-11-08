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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Push<T extends WordNumber> extends AbstractInstruction<T> {
  public OpcodeReference<T> getTarget() {
    return target;
  }

  public void setTarget(OpcodeReference<T> target) {
    this.target = target;
  }

  protected OpcodeReference<T> target;
  protected final Register<T> sp;
  protected final Memory<T> memory;

  public Push(OpcodeReference target, Register<T> sp, Memory<T> memory) {
    this.target = target;
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    doPush(target.read(), sp, memory);
    return 5 + cyclesCost;
  }

  public static <T extends WordNumber> void doPush(T value, Register<T> sp, Memory<T> memory) {
//    System.out.println("push: " + value);
    memory.disableWriteListener();
    sp.decrement();
    sp.decrement();
    T address = sp.read();
    Memory.write16Bits(memory, value, address);
    memory.enableWriteListener();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    visitor.visitPush(this);
  }
}

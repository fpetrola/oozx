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
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class RST<T extends WordNumber> extends AbstractInstruction<T>  implements JumpInstruction<T> {
  private final int p;
  private final ImmutableOpcodeReference<T> pc;
  private final Register<T> sp;
  private final Memory<T> memory;

  public RST(int p, ImmutableOpcodeReference<T> pc, Register<T> sp, Memory<T> memory) {
    this.p = p;
    this.pc = pc;
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    Push.doPush(pc.read().plus1(), sp, memory);
    setNextPC(WordNumber.createValue(p & 0xFFFF));
    return 5 + 3 + 3;
  }

  public String toString() {
    return "RST " + String.format("%02X", p);
  }

  public int getP() {
    return p;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingRst(this);
  }
}

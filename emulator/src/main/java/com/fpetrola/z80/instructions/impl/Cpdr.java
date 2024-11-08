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

import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Flags;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

public class Cpdr<T extends WordNumber> extends RepeatingInstruction<T> {
  private final Register<T> flag;

  public Cpdr(ImmutableOpcodeReference<T> pc, RegisterPair<T> bc, Register<T> flag, Cpd cpd) {
    super(cpd, pc, bc);
    this.flag = flag;
  }

  protected boolean checkLoopCondition() {
    return !((flag.read().intValue() & Flags.ZERO_FLAG) != 0) && bc.read().isNotZero();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitCpdr(this);
  }
}

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
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class DAA<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static AluOperation daaTableAluOperation = new AluOperation() {
    public int execute(int registerA, int carry, int flags) {
      // pc:4
      // The following algorithm is from comp.sys.sinclair's FAQ.
      int c, d;

      if (registerA > 0x99 || ((flags & FLAG_C) != 0)) {
        c = FLAG_C;
        d = 0x60;
      } else {
        c = d = 0;
      }

      if ((registerA & 0x0f) > 0x09 || ((flags & FLAG_H) != 0)) {
        d += 0x06;
      }

      int regA = ((flags & FLAG_N) != 0 ? registerA - d : registerA + d) & 0xFF;
      flags = TABLE_SZ[regA]
          | PARITY_TABLE[regA]
          | TABLE_XY[regA]
          | ((regA ^ registerA) & FLAG_H)
          | (flags & FLAG_N)
          | c;
      int Q = flags;

      return regA;
    }
  };

  public DAA(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, reg_A) -> daaTableAluOperation.executeWithCarry2(reg_A, reg_A, tFlagRegister.read().intValue(), tFlagRegister));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingDaa(this))
      super.accept(visitor);
  }
}

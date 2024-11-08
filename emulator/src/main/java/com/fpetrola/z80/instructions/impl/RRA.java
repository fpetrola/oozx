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

import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class RRA<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final AluOperation rraTableAluOperation = new AluOperation() {
    public int execute(int a, int carry) {
      boolean c = (a & 0x01) != 0;

      a = (a >> 1);
      if (getC())
        a = (a | 0x0080);
      if (c)
        setC();
      else
        resetC();
      resetH();
      resetN();

      return a;
    }
  };

  public RRA(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, regA) -> rraTableAluOperation.executeWithCarry(regA, tFlagRegister));
  }
}

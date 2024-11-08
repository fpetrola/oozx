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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Add16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation add16TableAluOperation = new AluOperation() {
    public int execute(int value2, int value, int carry) {
      int operand = value;
      int result = value2 + value; // ADD HL,rr
      resetN(); // N = 0;
      //
      int temp = (value2 & 0x0FFF) + (operand & 0x0FFF);
      if ((temp & 0xF000) != 0)
        setH();
      else
        resetH();
      if (result > lsw) // overflow ?
      {
        setC();
        return result & lsw;
      } else {
        resetC();
        return result;
      }
    }
  };

  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value2, value) -> add16TableAluOperation.executeWithCarry2(value2, value, tFlagRegister.read().intValue(), tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdd16(this);
  }
}

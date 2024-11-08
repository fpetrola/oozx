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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Add<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final TableAluOperation adc8TableAluOperation = new TableAluOperation() {
    public int execute(int a, int value, int carry) {
      data = carry;
      int reg_A = a;
      int local_reg_A = reg_A;
      setHalfCarryFlagAdd(local_reg_A, value, carry);
      setOverflowFlagAdd(local_reg_A, value, carry);
      local_reg_A = local_reg_A + value + carry;
      setS((local_reg_A & 0x0080) != 0);
      setC((local_reg_A & 0xff00) != 0);
      local_reg_A = local_reg_A & 0x00ff;
      setZ(local_reg_A == 0);
      resetN();
      reg_A = local_reg_A;
      setUnusedFlags(reg_A);
      return reg_A;
    }
  };

  public Add(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value, regA) -> adc8TableAluOperation.executeWithoutCarry(value, regA, tFlagRegister));
  }

  @Override
  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();
    target.write(binaryAluOperation.execute(flag, value1, value2));
    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdd(this);
  }
}

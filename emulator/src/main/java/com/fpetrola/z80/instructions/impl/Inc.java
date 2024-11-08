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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Inc<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final TableAluOperation inc8TableAluOperation = new TableAluOperation() {
    public int execute(int a, int carry) {
      data = carry;
      int value = a;
      setHalfCarryFlagAdd(value, 1);
      setPV(value == 0x7F);
      value++;
      setS((value & 0x0080) != 0);
      value = value & 0x00ff;
      setZ(value == 0);
      setUnusedFlags(value);

      return value;
    }
  };

  public Inc(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, value) -> inc8TableAluOperation.executeWithCarry(value, tFlagRegister));
    this.flag = flag;
  }

  @Override
  public int execute() {
    final T value2 = target.read();
    T execute = unaryAluOperation.execute(flag, value2);
    target.write(execute);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingInc(this))
      super.accept(visitor);
  }
}

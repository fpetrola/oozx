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
import com.fpetrola.z80.registers.flag.*;

public class Xor<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  protected static final AluOperation xorTableAluOperation = new AluOperation() {
    public int execute(int result, int value, int carry) {
      setS((result & 0x0080) != 0);
      setZ(result == 0);
      setPV(parity[result & 0xFF]);
      setUnusedFlags(result);
      return result;
    }
  };

  public Xor(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (flag1, value1, value2) -> WordNumber.createValue(0));
  }

  @Override
  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();

    T result = value1.xor(value2);

    T i = xorTableAluOperation.executeWithoutCarry(value1, result, flag);

    target.write(i);
    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingXor(this);
  }
}

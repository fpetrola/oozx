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

public class RLC<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {

  public static final TableAluOperation rlcTableAluOperation1 = new TableAluOperation() {
    public int execute(int a, int carry) {
      data = carry;

      a = a << 1;
      if ((a & 0x0FF00) != 0) {
        setC();
        a = a | 0x01;
      } else
        resetC();
      // standard flag updates
      if ((a & FLAG_S) == 0)
        resetS();
      else
        setS();
      if ((a & 0x00FF) == 0)
        setZ();
      else
        resetZ();
      resetH();
      resetN();
      // put value back
      a = a & 0x00FF;
      setPV(parity[a]);

      return a;
    }
  };

  public RLC(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, temp1) -> rlcTableAluOperation1.executeWithCarry(temp1, tFlagRegister));
  }

  public int execute() {
    final T value2 = target.read();
    T execute = unaryAluOperation.execute(flag, value2);
    target.write(execute);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingRlc(this))
      super.accept(visitor);
  }
}

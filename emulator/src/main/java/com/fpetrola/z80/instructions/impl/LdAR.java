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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class LdAR<T extends WordNumber> extends Ld<T> {
  public static final AluOperation ldarTableAluOperation = new TableAluOperation() {
    public int execute(int reg_R, int reg_A, int carry) {
      reg_A = reg_R & 0x7F;
      setS((reg_A & FLAG_S) != 0);
      setZ(reg_A == 0);
      resetH();
      resetN();
      setPV(carry == 1);

      return reg_A;
    }
  };
  private final State<T> state;

  public LdAR(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag, State<T> state) {
    super(target, source, flag);
    this.state = state;
  }

  public int execute() {
    T value = source.read();
    T reg_A = target.read();
    boolean iff2 = state.isIff2();
    T ldar = ldarTableAluOperation.executeWithCarry2(reg_A, value, iff2 ? 1 : 0, flag);

    target.write(ldar);

    return cyclesCost;
  }
}

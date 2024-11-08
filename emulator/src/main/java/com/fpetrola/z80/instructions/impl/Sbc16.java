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

public class Sbc16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation sbc16TableAluOperation = new AluOperation() {
    public int execute(int HL, int DE, int carry) {
      data = carry;
      int a = HL;
      int b = DE;
      int c = getC() ? 1 : 0;
      int lans = (a - b) - c;
      int ans = lans & 0xffff;
      setS((ans & (FLAG_S << 8)) != 0);
      setZ(ans == 0);
      setC(lans < 0);
      // setPV( ((a ^ b) & (a ^ value) & 0x8000)!=0 );
      setOverflowFlagSub16(a, b, c);
      if ((((a & 0x0fff) - (b & 0x0fff) - c) & 0x1000) != 0)
        setH();
      else
        resetH();
      setN();

      return ans;
    }
  };

  public Sbc16(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, DE, HL) -> sbc16TableAluOperation.executeWithCarry(DE, HL, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingSbc16(this);
  }
}

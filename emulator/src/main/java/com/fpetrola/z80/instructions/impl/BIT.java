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

import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  public static final AluOperation testBitTableAluOperation = new AluOperation() {
    public int execute(int bit, int value, int carry) {
      resetS();

      switch (bit) {
        case 0: {
          value = value & setBit0;
          break;
        }
        case 1: {
          value = value & setBit1;
          break;
        }
        case 2: {
          value = value & setBit2;
          break;
        }
        case 3: {
          value = value & setBit3;
          break;
        }
        case 4: {
          value = value & setBit4;
          break;
        }
        case 5: {
          value = value & setBit5;
          break;
        }
        case 6: {
          value = value & setBit6;
          break;
        }
        case 7: {
          value = value & setBit7;
          setS(value != 0);
          break;
        }
      }
      setZ(0 == value);
      setPV(0 == value);
      resetN();
      setH();

      return value;
    }
  };

  public BIT(OpcodeReference target, int n, Register<T> flag) {
    super(target, n, flag);
  }

  public int execute() {
    final T value = target.read();
    testBitTableAluOperation.executeWithCarry(value, WordNumber.createValue(n), flag);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }
}

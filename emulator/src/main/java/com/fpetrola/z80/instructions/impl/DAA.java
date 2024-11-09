/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
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

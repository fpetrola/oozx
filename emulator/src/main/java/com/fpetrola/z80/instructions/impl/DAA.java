/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class DAA extends ParameterizedUnaryAluInstruction {
  public final static AluOperation daaTableAluOperation = new TableAluOperation() {
    public int execute(int value1, int value2, int flags) {
      F = value1;
      value2 &= 0xff;
      int add = 0;
      int carry = (F & FLAG_C);
      if (((F & FLAG_H) != 0) || ((value2 & 0x0f) > 9)) add = 6;
      if (carry != 0 || (value2 > 0x99)) add |= 0x60;
      if (value2 > 0x99) carry = FLAG_C;
      Register f = new Plain8BitRegister("");
      f.write(F);
      if ((F & FLAG_N) != 0) {
        value2 = Sub.sub8TableAluOperation.execute2Values(add, value2, f);
      } else {
        value2 = Add.add8TableAluOperation.execute2Values(add, value2, f);
      }
      F = f.read();

      F = (F & ~(FLAG_C | FLAG_P)) | carry | parityTable(value2);
      Q = F;

      return value2;
    }
  };

  public DAA(OpcodeReference target, Register flag) {
    super(target, flag, (reg_A) -> daaTableAluOperation.execute1ValueAndCarry(reg_A, flag.read(), flag));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingDaa(this))
      super.accept(visitor);
  }
}

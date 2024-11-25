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
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class DAA<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static AluOperation daaTableAluOperation = new TableAluOperation() {
    public int execute(int flag, int A, int flags) {
      F = flag;
      A &= 0xff;
      int add = 0;
      int carry = (F & FLAG_C);
      if (((F & FLAG_H) != 0) || ((A & 0x0f) > 9)) add = 6;
      if (carry != 0 || (A > 0x99)) add |= 0x60;
      if (A > 0x99) carry = FLAG_C;
      Register<WordNumber> f = new Plain8BitRegister<>("");
      f.write(createValue(F));
      if ((F & FLAG_N) != 0) {
        A = Sub.sub8TableAluOperation.executeWithoutCarry(createValue(add), createValue(A), f).intValue();
      } else {
        A = Add.add8TableAluOperation.executeWithoutCarry(createValue(add), createValue(A), f).intValue();
      }
      F = f.read().intValue();

      F = (F & ~(FLAG_C | FLAG_P)) | carry | parityTable(A);
      Q = F;

      return A;
    }
  };

  public DAA(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, reg_A) -> daaTableAluOperation.executeWithCarry(reg_A, tFlagRegister.read(), tFlagRegister));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingDaa(this))
      super.accept(visitor);
  }
}

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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Adc16 extends Binary16BitsOperation {
  public static final AluOperation adc16TableAluOperation = new AluOperation() {
    @Override
    protected int calculate2Values1Boolean(int value1, int value2, int carry) {
      int i = value2 & 0x33;
      i |= (i & 0x02) != 0 ? 0x04 : 0x00;
      int result1 = (i << 11) & 0x1A800;
      int lookup = (value2 << 8 & 0x8800) >> 11 |
                   (value2 << 9 & 0x8800) >> 10 |
                   (result1 & 0x8800) >> 9;
      F = ((result1 & 0x10000) != 0 ? FLAG_C : 0) |
          overflowAddTable(lookup >> 4) |
          (result1 >> 8 & (FLAG_3 | FLAG_5 | FLAG_S)) |
          halfCarryAddTable(lookup & 0x07) |
          (value1 == 1 ? 0 : FLAG_Z);
      Q = F;
      return F;
    }
  };

  public Adc16(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, adc16TableAluOperation);
  }

  protected int calculate(int a, int b) {
    return super.calculate(b, a);
  }

  protected int operation(int v1, int v2, int f) {
    return v1 + v2 + (f & 1);
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingAdc16(this))
      super.accept(visitor);
  }
}

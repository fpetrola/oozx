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

public class Sbc16 extends Binary16BitsOperation {
  public static final AluOperation sbc16TableAluOperation = new AluOperation() {
        @Override
        protected int calculate2Values1Boolean(int value1, int value2, int carry) {
          int i = value2 & 0x33;
          i |= i << 1 & 0x04;
          int result1 = i << 11 & 0x1A800;
          int lookup = (value2 << 8 & 0x8800) >> 11 |
                       (value2 << 9 & 0x8800) >> 10 |
                       (result1 & 0x8800) >> 9;
          F = ((result1 & 0x10000) != 0 ? FLAG_C : 0) |
              FLAG_N | overflowSubTable(lookup >> 4) |
              (result1 >> 8 & (FLAG_3 | FLAG_5 | FLAG_S)) |
              halfCarrySubTable(lookup & 0x07) |
              (value1 != 0 ? 0 : FLAG_Z);
          Q = F;
          return F;
        }

        //    public int execute(int HL, int value, int carry) {
        //      F = carry & 0xFF;
        //      int sub16temp = HL - (value) - (F & FLAG_C);
        //      int lookup = ((HL & 0x8800) >> 11) |
        //          (((value) & 0x8800) >> 10) |
        //          ((sub16temp & 0x8800) >> 9);
        //      HL = sub16temp;
        //      int H = (HL >> 8) & 0xff;
        //      F = ((sub16temp & 0x10000) != 0 ? FLAG_C : 0) |
        //          FLAG_N | overflowSubTable(lookup >> 4) |
        //          (H & (FLAG_3 | FLAG_5 | FLAG_S)) |
        //          halfCarrySubTable(lookup & 0x07) |
        //          (HL != 0 ? 0 : FLAG_Z);
        //      Q = F;
        //
        //      return sub16temp & 0xffff;
        //    }
      };
  public Sbc16(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, sbc16TableAluOperation);
  }

  @Override
  protected int doExecute(int sourceValue, int targetValue) {
    return calculate(flag, targetValue, sourceValue,
        (v1, v2, f) -> v1 - v2 - (f & 1),
        (f1, value3, value2, result1) -> aluOperation.execute2ValuesAndCarry(result1 != 0 ? 1 : 0, value3, flag));
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingSbc16(this))
      super.accept(visitor);
  }
}

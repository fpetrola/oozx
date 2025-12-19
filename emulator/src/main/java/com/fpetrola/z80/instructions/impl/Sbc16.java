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
  public static class Sbc16TableAluOperation extends AluOperation {
    protected int calculate2Values1Boolean(int value1, int value2, int carry) {
      int[] decompressed = ProcessorUtils.decompress(value1, value2);
      F = decompressed[2];
      return calculateOriginal(decompressed[0], decompressed[1], decompressed[3], decompressed[4]);
    }

    private int calculateOriginal(int value1, int value2, int sub16temp, int value1NotZero) {
      int lookup = ((value1 & 0x8800) >> 11) |
                   ((value2 & 0x8800) >> 10) |
                   ((sub16temp & 0x8800) >> 9);
      F = ((sub16temp & 0x10000) != 0 ? FLAG_C : 0) |
          FLAG_N | overflowSubTable(lookup >> 4) |
          (sub16temp >> 8 & (FLAG_3 | FLAG_5 | FLAG_S)) |
          halfCarrySubTable(lookup & 0x07) |
          (value1NotZero != 0 ? 0 : FLAG_Z);
      Q = F;
      return F;
    }
  }

  public Sbc16(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, new Sbc16TableAluOperation());
  }

  protected int operation(int v1, int v2, int f) {
    return v1 - v2 - (f & 1);
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingSbc16(this))
      super.accept(visitor);
  }
}

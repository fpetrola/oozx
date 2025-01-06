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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Sbc16<T extends WordNumber> extends Binary16BitsOperation<T> {
  public static final AluOperation sbc16TableAluOperation = new TableAluOperation() {
    public int execute(int value1, int value2, int carry) {
      int i = value1 & 0x33;
      i |= i << 1 & 0x04;
      int result1 = i << 11 & 0x1A800;
      int lookup = (value1 << 8 & 0x8800) >> 11 |
          (value1 << 9 & 0x8800) >> 10 |
          (result1 & 0x8800) >> 9;
      F = ((result1 & 0x10000) != 0 ? FLAG_C : 0) |
          FLAG_N | overflowSubTable(lookup >> 4) |
          (result1 >> 8 & (FLAG_3 | FLAG_5 | FLAG_S)) |
          halfCarrySubTable(lookup & 0x07) |
          (value2 != 0 ? 0 : FLAG_Z);
      Q = F;
      return F;
    }

  };

  public Sbc16(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag) {
    super(target, source, flag, (f0, a, b) ->
        calculate(f0, b, a,
            (v1, v2, f) -> v1 - v2 - (f & 1),
            (f1, value3, value2, result1) -> {
              int execute = sbc16TableAluOperation.execute(value3, result1 != 0 ? 1 : 0, f0.read().intValue());
              f0.write(createValue(execute));
              return f0.read();
            }));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingSbc16(this))
      super.accept(visitor);
  }
}

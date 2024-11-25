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
import org.apache.commons.lang3.function.TriFunction;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Add16<T extends WordNumber> extends Binary16BitsOperation<T> {
  public static final AluOperation add16TableAluOperation = new TableAluOperation() {
    public int execute(int flag, int value1AndAddition, int value2Bit0) {
      F = flag;
      getValue1(value1AndAddition << 4, value2Bit0 << 11, value1AndAddition << 11);
      return F;
    }

    private void getValue1(int value1, int value2, int add16temp) {
      int lookup = ((value1 & 0x0800) >> 11) |
          ((value2 & 0x0800) >> 10) |
          ((add16temp & 0x0800) >> 9);
      F = (F & (FLAG_V | FLAG_Z | FLAG_S)) |
          ((add16temp & 0x10000) != 0 ? FLAG_C : 0) |
          ((add16temp >> 8) & (FLAG_3 | FLAG_5)) |
          halfCarryAddTable(lookup);
      Q = F;
    }
  };

  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (flag0, a, b) ->
        calculate(flag0, b, a,
            (v1, v2, f) -> v1 + v2,
            (flag1, value3, value2, result1) -> add16TableAluOperation.executeWithCarry2(createValue(value3), createValue(flag1.read().intValue()), value2 >> 11, flag0),
            (v3, v4, result2) -> (v3 & 0x0800) >> 4 | result2 >> 11));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingAdd16(this))
      super.accept(visitor);
  }
}

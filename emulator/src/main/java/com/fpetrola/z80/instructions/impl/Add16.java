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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Add16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation add16TableAluOperation = new TableAluOperation() {
    public int execute(int add16temp1, int value1, int value2) {
      F = add16temp1;
      getValue1((value1 & 1) << 11, (value1 & 2) << 10, value1 << 9);
      return F;
    }

    private int getValue1(int value1, int value2, int add16temp) {
      int lookup = ((value1 & 0x0800) >> 11) |
          ((value2 & 0x0800) >> 10) |
          ((add16temp & 0x0800) >> 9);
      value1 = add16temp;
      F = (F & (FLAG_V | FLAG_Z | FLAG_S)) |
          ((add16temp & 0x10000) != 0 ? FLAG_C : 0) |
          ((add16temp >> 8) & (FLAG_3 | FLAG_5)) |
          halfCarryAddTable[lookup];
      Q = F;
      return value1;
    }
  };

  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, v2, v1) -> {
      int value1 = v1.intValue();
      int value2 = v2.intValue();
      int add16temp = value1 + value2;
      int i = value1 >> 11 & 0x01 | value2 >> 10 & 0x02 | ((add16temp & 0x13800) >> 9);
      T t = add16TableAluOperation.executeWithCarry2(createValue(i), createValue(flag.read().intValue()), 1, tFlagRegister);
      flag.write(t);
      return createValue(add16temp);
    });
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdd16(this);
  }
}

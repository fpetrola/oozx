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

public class Adc16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation adc16TableAluOperation = new AluOperation() {
    public int execute(int value1, int value2, int carry) {
      int result = value1 + value2 + (F & FLAG_C);
      calc(value1 & 0x8800, value2 & 0x8800, result & 0x1A800, result != 0);
      return result;
    }

    private void calc(int value1, int value2, int result, boolean resultIsZero) {
      int lookup = ((value1 & 0x8800) >> 11) |
          ((value2 & 0x8800) >> 10) |
          ((result & 0x8800) >> 9);
      F = ((result & 0x10000) != 0 ? FLAG_C : 0) |
          overflowAddTable[lookup >> 4] |
          ((result >> 8) & (FLAG_3 | FLAG_5 | FLAG_S)) |
          halfCarryAddTable[lookup & 0x07] |
          (resultIsZero ? 0 : FLAG_Z);
      Q = F;
    }
  };

  public Adc16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, a, b) -> adc16TableAluOperation.executeWithCarry(a, b, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingAdc16(this))
      super.accept(visitor);
  }
}

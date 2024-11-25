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
import org.apache.commons.lang3.function.TriFunction;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Operation16Bits<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public Operation16Bits(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag, BinaryAluOperation<T> binaryAluOperation) {
    super(target, source, flag, binaryAluOperation);
  }

  protected static <T extends WordNumber> T calculate(Register<T> tFlagRegister, T a, T b, TriFunction<Integer, Integer, Integer, Integer> operation, TriFunction<Register<T>, Integer, Integer, T> action) {
    int value1 = a.intValue();
    int value2 = b.intValue();
    T flagValue = tFlagRegister.read();
    int result = operation.apply(value1, value2, flagValue.intValue());
    value1 = ((value1 & 0x8800 | (value2 & 0x8800) >> 1) | (result & 0x1A800 | (result & 0x2000) >> 1) >> 3) >> 8;
    action.apply(tFlagRegister, value1, result);
    return createValue(result & 0xffff);
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingOperation16Bits(this))
      super.accept(visitor);
  }
}

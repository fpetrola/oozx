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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.ToPrimitiveIntBiAndBooleanFunction;

public class Binary16BitsOperation extends ParameterizedBinaryAluInstruction {
  public Binary16BitsOperation(OpcodeReference target, ImmutableOpcodeReference source, Register flag, BinaryAluOperation binaryAluOperation) {
    super(target, source, flag, binaryAluOperation);
  }

  protected static  int calculate(Register tFlagRegister, int a, int b, ToPrimitiveIntBiAndBooleanFunction operation, Binary16BitsAluOperation  action) {
    return calculate(tFlagRegister, a, b, operation, action, (v1, v2, result1) -> ((v1 & 0x8800 | (v2 & 0x8800) >> 1) | (result1 & 0x1A800 | (result1 & 0x2000) >> 1) >> 3) >> 8);
  }

  protected static  int calculate(Register tFlagRegister, int a, int b, ToPrimitiveIntBiAndBooleanFunction operation, Binary16BitsAluOperation action, ToPrimitiveIntBiAndBooleanFunction compressFunction) {
    int value1 = a;
    int value2 = b;
    int flagValue = tFlagRegister.read();
    int result = operation.applyAsInt(value1, value2, flagValue);
    value1 = compressFunction.applyAsInt(value1, value2, result);
    action.execute(tFlagRegister, value1, value2, result);
    return result & 0xffff;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visiting16BitsOperation(this))
      super.accept(visitor);
  }

  interface Binary16BitsAluOperation {
    int execute (Register flag, int value1, int value2, int result);
  }
}

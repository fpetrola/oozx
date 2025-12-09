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
import com.fpetrola.z80.registers.flag.AluOperation;

public class Binary16BitsOperation extends ParameterizedBinaryAluInstruction {
  public Binary16BitsOperation(OpcodeReference target, ImmutableOpcodeReference source, Register flag, AluOperation aluOperation) {
    super(target, source, flag, aluOperation);
  }

  protected int calculate(int a, int b) {
    int result = operation(a, b, flag.read());
    executeAction(compress(a, b, result), b, result);
    return result & 0xffff;
  }

  protected int doExecute(int sourceValue, int targetValue) {
    return calculate(targetValue, sourceValue);
  }

  protected int compress(int v1, int v2, int result) {
    return ((v1 & 0x8800 | (v2 & 0x8800) >> 1) | (result & 0x1A800 | (result & 0x2000) >> 1) >> 3) >> 8;
  }

  protected void executeAction(int v1, int v2, int result) {
    aluOperation.execute2Values1Boolean(result != 0 ? 1 : 0, v1, flag.read() & 1, flag);
  }

  protected int operation(int v1, int v2, int f) {
    return 0;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visiting16BitsOperation(this))
      super.accept(visitor);
  }

  interface Binary16BitsAluOperation {
    int execute(Register flag, int value1, int value2, int result);
  }
}

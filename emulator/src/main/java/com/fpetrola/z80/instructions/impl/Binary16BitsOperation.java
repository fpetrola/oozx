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

  protected int doExecute(int sourceValue, int targetValue) {
    int f = flag.read() & 0xff;
    int result = operation(targetValue, sourceValue, f);
    int[] compressedParameters = ProcessorUtils.compressParameters(targetValue, sourceValue, result, f);
    aluOperation.execute2Values1Boolean(compressedParameters[0], compressedParameters[1], f, flag);
    return result & 0xffff;
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

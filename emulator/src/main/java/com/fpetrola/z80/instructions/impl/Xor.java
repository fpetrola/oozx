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
import com.fpetrola.z80.registers.flag.*;

public class Xor<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  protected static final AluOperation xorTableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int carry) {
      A ^= (value);
      F = sz53pTable(A);
      Q = F;
      return A;
    }
  };

  public Xor(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (flag1, value1, value2) -> xorTableAluOperation.executeWithoutCarry(value2, value1, flag1));
  }

  @Override
  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();
    target.write(binaryAluOperation.execute(flag, value1, value2));
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingXor(this);
  }
}

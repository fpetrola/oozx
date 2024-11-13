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

public class Xor extends ParameterizedBinaryAluInstruction {
  public static class XorTableAluOperation extends AluOperation {
    protected int calculate2Values1Boolean(int value1, int value2, int carry) {
      value2 ^= (value1);
      F = sz53pTable(value2);
      Q = F;
      return value2;
    }
  }

  public Xor(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, new XorTableAluOperation());
  }

  public void accept(InstructionVisitor<?> visitor) {
    super.accept(visitor);
    visitor.visitingXor(this);
  }
}

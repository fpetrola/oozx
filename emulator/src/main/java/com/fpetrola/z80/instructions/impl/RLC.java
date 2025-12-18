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
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class RLC extends ParameterizedUnaryAluInstruction {
  public static class RlcTable1AluOperation extends AluOperation {
    protected int calculate1Value(int value) {
      value = (value << 1 | value >> 7) & 0xff;
      F = (value & FLAG_C) | sz53pTable(value);
      Q = F;
      return value;
    }
  }

  public RLC(OpcodeReference target, Register flag) {
    super(target, flag, new RlcTable1AluOperation());
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingRlc(this))
      super.accept(visitor);
  }
}

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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Add16 extends Binary16BitsOperation {
  public static class Add16TableAluOperation extends AluOperation {
    protected int calculate2Values1Boolean(int value1, int value2, int value2Bit0) {
      F = value2;
      getValue1(value1 << 4, value2Bit0 << 11, value1 << 11);
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
  }

  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, new Add16TableAluOperation());
  }

  protected int operation(int v1, int v2, int f) {
    return v1 + v2;
  }

  protected void executeAction(int v1, int v2, int result) {
    aluOperation.execute2Values1Boolean(v1, flag.read(), v2 >> 11, flag);
  }

  protected int compress(int v1, int v2, int result) {
    return (v1 & 0x0800) >> 4 | result >> 11;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingAdd16(this))
      super.accept(visitor);
  }
}

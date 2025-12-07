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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Sub extends ParameterizedBinaryAluInstruction {
  public static final TableAluOperation sub8TableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value1, int value2, int carry) {
      int subtemp = value2 - value1;
      int lookup = ((value2 & 0x88) >> 3) | ((value1 & 0x88) >> 2) | ((subtemp & 0x88) >> 1);
      value2 = subtemp & 0xff;
      F = ((subtemp & 0x100) != 0 ? FLAG_C : 0) | FLAG_N |
          halfCarrySubTable(lookup & 0x07) | overflowSubTable(lookup >> 4) | sz53Table(value2);
      Q = F;

      return value2;
    }
  };

  public Sub(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag,  sub8TableAluOperation);
  }

  protected int doExecute(int sourceValue, int targetValue) {
    return super.doExecute(targetValue, sourceValue);
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingSub(this);
  }
}

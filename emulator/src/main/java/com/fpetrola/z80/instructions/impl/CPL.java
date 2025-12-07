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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class CPL extends ParameterizedUnaryAluInstruction {
  public static final TableAluOperation cplTableAluOperation = new TableAluOperation() {
    public int calculate1Value1Boolean(int A, int carry) {
      A ^= 0xff;
      F = (F & (FLAG_C | FLAG_P | FLAG_Z | FLAG_S)) |
          (A & (FLAG_3 | FLAG_5)) | (FLAG_N | FLAG_H);
      Q = F;

      return A;
    }
  };

  public CPL(OpcodeReference target, Register flag) {
    super(target, flag, cplTableAluOperation);
  }

  @Override
  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingCpl(this))
      super.accept(visitor);
  }
}

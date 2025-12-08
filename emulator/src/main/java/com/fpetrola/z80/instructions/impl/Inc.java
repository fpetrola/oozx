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

public class Inc extends ParameterizedUnaryAluInstruction {
  public static final TableAluOperation inc8TableAluOperation = new TableAluOperation() {
    public int calculate1Value(int value) {
      (value)++;
      value &= 0xff;
      F = (F & FLAG_C) | ((value) == 0x80 ? FLAG_V : 0) |
          (((value) & 0x0f) != 0 ? 0 : FLAG_H) | sz53Table((value));
      Q = F;
      return value;
    }
  };

  public Inc(OpcodeReference target, Register flag) {
    super(target, flag, inc8TableAluOperation);
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingInc(this))
      super.accept(visitor);
  }
}

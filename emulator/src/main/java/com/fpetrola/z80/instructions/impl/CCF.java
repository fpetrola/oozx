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

import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class CCF<T extends WordNumber> extends DefaultTargetFlagInstruction<T> {
  public static final AluOperation ccfTableAluOperation = new TableAluOperation() {
    public int execute(int data1, int a, int carry) {
      data = (data1 & (PARITY_MASK | ZERO_MASK | SIGN_MASK)) | ((data1 & CARRY_MASK) != 0 ? HALFCARRY_MASK : CARRY_MASK) | (a & (FLAG_3 | FLAG_5));
      return data;
    }
  };

  public CCF(Register flag, Register<T> a) {
    super(a, flag);
  }

  public int execute() {
    ccfTableAluOperation.executeWithCarry(target.read(), flag.read(), flag);
    return 4;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingCcf(this);
  }

}

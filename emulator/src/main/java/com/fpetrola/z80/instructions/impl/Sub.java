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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Sub<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final TableAluOperation sub8TableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int carry) {
      int subtemp = A - value;
      int lookup = ((A & 0x88) >> 3) | ((value & 0x88) >> 2) | ((subtemp & 0x88) >> 1);
      A = subtemp & 0xff;
      F = ((subtemp & 0x100) != 0 ? FLAG_C : 0) | FLAG_N |
          halfcarry_sub_table[lookup & 0x07] | overflow_sub_table[lookup >> 4] | sz53_table[A];
      Q = F;

      return A;
    }
  };

  public Sub(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value, reg_A) -> sub8TableAluOperation.executeWithoutCarry(value, reg_A, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingSub(this);
  }
}

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
import com.fpetrola.z80.registers.flag.AluOperation;

public class Sbc16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation sbc16TableAluOperation = new AluOperation() {
    public int execute(int HL, int value, int carry) {
      F = carry;
      int sub16temp = HL - (value) - (F & FLAG_C);
      int lookup = ((HL & 0x8800) >> 11) | (((value) & 0x8800) >> 10) | ((sub16temp & 0x8800) >> 9);
      HL = sub16temp;
      F = ((sub16temp & 0x10000) != 0 ? FLAG_C : 0) |
          FLAG_N | overflow_sub_table[lookup >> 4] |
          ((HL >> 8) & (FLAG_3 | FLAG_5 | FLAG_S)) |
          halfcarry_sub_table[lookup & 0x07] |
          (HL != 0 ? 0 : FLAG_Z);
      Q = F;
      return HL;
    }
  };

  public Sbc16(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, DE, HL) -> sbc16TableAluOperation.executeWithCarry(DE, HL, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingSbc16(this);
  }
}

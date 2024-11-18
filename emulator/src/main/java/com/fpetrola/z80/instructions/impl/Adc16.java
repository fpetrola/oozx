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

public class Adc16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation adc16TableAluOperation = new AluOperation() {
    public int execute(int b, int a, int carry) {
      int res = a + b;
      if (carry == 1) res++;
      int i = res > 0xffff ? 1 : 0;
      res &= 0xffff;
      data = sz53n_addTable[res >> 8];
      if (res != 0) data &= ~ZERO_MASK;
      if (((res ^ a ^ b) & 0x1000) != 0) data |= HALFCARRY_MASK;
      if (((a ^ ~b) & (a ^ res)) > 0x7fff) data |= OVERFLOW_MASK;
      flagQ = true;
      data = data | i;
      return res;
    }
  };

  public Adc16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, a, b) -> adc16TableAluOperation.executeWithCarry(a, b, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingAdc16(this))
      super.accept(visitor);
  }
}

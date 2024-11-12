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

public class Add16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation add16TableAluOperation = new AluOperation() {
    public int execute(int reg16, int oper16, int carry) {
      oper16 += reg16;
      int i = oper16 > 0xffff ? 1 : 0;
      data = (data & FLAG_SZP_MASK) | ((oper16 >>> 8) & FLAG_53_MASK);
      oper16 &= 0xffff;
      if ((oper16 & 0x0fff) < (reg16 & 0x0fff)) data |= HALFCARRY_MASK;
      memptr = reg16 + 1;
      flagQ = true;
      data = data | i;
      return oper16;
    }
  };

  public Add16(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value2, value) -> add16TableAluOperation.executeWithCarry2(value2, value, tFlagRegister.read().intValue(), tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdd16(this);
  }
}

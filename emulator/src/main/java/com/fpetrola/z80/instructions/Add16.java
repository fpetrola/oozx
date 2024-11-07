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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.instructions.base.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Add16<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final AluOperation add16TableAluOperation = new AluOperation() {
    public int execute(int value2, int value, int carry) {
      int operand = value;
      int result = value2 + value; // ADD HL,rr
      resetN(); // N = 0;
      //
      int temp = (value2 & 0x0FFF) + (operand & 0x0FFF);
      if ((temp & 0xF000) != 0)
        setH();
      else
        resetH();
      if (result > lsw) // overflow ?
      {
        setC();
        return result & lsw;
      } else {
        resetC();
        return result;
      }
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

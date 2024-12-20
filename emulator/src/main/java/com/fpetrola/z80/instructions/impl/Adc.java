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

public class Adc<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final TableAluOperation adc8TableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int carry) {
      F = carry;
      int adctemp = A + (value) + (F & FLAG_C);
      int lookup = ((A & 0x88) >> 3) |
          (((value) & 0x88) >> 2) |
          ((adctemp & 0x88) >> 1);
      A = adctemp & 0xff;
      F = ((adctemp & 0x100) != 0 ? FLAG_C : 0) |
          halfCarryAddTable(lookup & 0x07) | overflowAddTable(lookup >> 4) |
          sz53Table(A);
      Q = F;
      return A;
    }
  };
  public Adc(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value, regA) -> adc8TableAluOperation.executeWithCarry(value, regA, tFlagRegister));
  }

  @Override
  public int execute() {
    return super.execute();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdc(this);
  }
}

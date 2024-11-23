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

import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RRA<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final AluOperation rraTableAluOperation = new TableAluOperation() {
    public int execute(int value, int a, int carry) {
      F = value;
      int A = a;
      int bytetemp = A;
      A = (A >> 1) | (F << 7);
      F = (F & (FLAG_P | FLAG_Z | FLAG_S)) | (A & (FLAG_3 | FLAG_5)) | (bytetemp & FLAG_C);
      Q = F;
      return A;
    }
  };

  public RRA(OpcodeReference target, Register<T> flag) {
    super(target, flag, (flagRegister, regA) -> rraTableAluOperation.executeWithCarry(regA, flagRegister.read(), flagRegister));
  }
}

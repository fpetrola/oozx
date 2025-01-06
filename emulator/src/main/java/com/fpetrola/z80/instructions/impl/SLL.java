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
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class SLL<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final TableAluOperation sllTableAluOperation = new TableAluOperation() {
    public int execute(int value, int flag, int carry) {
      F = value >> 7;
      value = (value << 1) | 0x01;
      value &= 0xff;
      F |= sz53pTable(value);
      Q = F;
      return value;
    }
  };

  public SLL(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, a) -> sllTableAluOperation.executeWithCarry(a, tFlagRegister));
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingSll(this))
      super.accept(visitor);
  }
}

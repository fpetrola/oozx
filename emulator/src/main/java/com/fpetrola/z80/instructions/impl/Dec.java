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

public class Dec<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final TableAluOperation dec8TableAluOperation = new TableAluOperation() {
    public int execute(int value, int flag, int carry) {
      F = flag;
      F = (F & FLAG_C) | (((value) & 0x0f) != 0 ? 0 : FLAG_H) | FLAG_N;
      (value)--;
      value &= 0xff;
      F |= ((value) == 0x7f ? FLAG_V : 0) | sz53Table(value);
      Q = F;
      return value;
    }
  };

  public Dec(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, value) -> dec8TableAluOperation.executeWithCarry(value, tFlagRegister));
  }

  public int execute() {
    final T value2 = target.read();
    T execute = unaryAluOperation.execute(flag, value2);
    target.write(execute);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingDec(this))
      super.accept(visitor);
  }
}

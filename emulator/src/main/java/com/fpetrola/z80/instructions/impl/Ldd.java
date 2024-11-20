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
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Ldd<T extends WordNumber> extends Ldi<T> {
  public static final AluOperation lddTableAluOperation = new TableAluOperation() {
    public <T extends WordNumber> T executeWithCarry2(T value, T regA, int carry, Register<T> flag) {
      F = flag.read().intValue();
      resetH();
      resetN();
      setPV(carry != 0);
      set3(((value.intValue() + regA.intValue()) & BIT3_MASK) != 0);
      set5(((value.intValue() + regA.intValue()) & 0x02) != 0);
      return WordNumber.createValue(F);
    }
  };

  public Ldd(Register<T> de, RegisterPair<T> bc, RegisterPair<T> hl, Register<T> flag, Memory<T> memory, IO<T> io, Register<T> a) {
    super(de, bc, hl, flag, memory, io, a);
  }

  protected void flagOperation(T valueFromHL) {
    flag.write(lddTableAluOperation.executeWithCarry2(valueFromHL, a.read(), bc.read().intValue() != 0 ? 1 : 0, flag));
  }

  protected void next() {
    hl.decrement();
    de.decrement();
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitLdd(this))
      super.accept(visitor);
  }
}

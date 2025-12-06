/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Ldd extends Ldi {
  public static final AluOperation lddTableAluOperation = new TableAluOperation() {
    public  int execute2Values1Boolean(int value1, int value2, int booleanValue, Register flag) {
      F = flag.read();
      int A = value2;
      int BC = booleanValue;
      int bytetemp = value1;
      bytetemp += A;
      F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC != 0 ? FLAG_V : 0) |
          (bytetemp & FLAG_3) | ((bytetemp & 0x02) != 0 ? FLAG_5 : 0);
      Q = F;

      return F;
    }
  };

  public Ldd(Register de, RegisterPair bc, RegisterPair hl, Register flag, Memory memory, IO io, Register a) {
    super(de, bc, hl, flag, memory, io, a);
  }

  protected void flagOperation(int valueFromHL) {
    flag.write(lddTableAluOperation.execute2Values1Boolean(valueFromHL, a.read(), bc.read() != 0 ? 1 : 0, flag));
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

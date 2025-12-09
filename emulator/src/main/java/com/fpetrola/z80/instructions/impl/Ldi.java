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
import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Ldi extends BlockInstruction {
  public static final AluOperation ldiTableAluOperation = new AluOperation() {
    protected int calculate2Values1Boolean(int value1, int value2, int carry) {
      F = value1;
      int BC = carry;
      int bytetemp = value2;
      F = (F & (FLAG_C | FLAG_Z | FLAG_S)) | (BC != 0 ? FLAG_V : 0) |
          (bytetemp & FLAG_3) | ((bytetemp & 0x02) != 0 ? FLAG_5 : 0);
      Q = F;

      return F;
    }
  };
  protected final Register a;

  public Register getDe() {
    return de;
  }

  public void setDe(Register de) {
    this.de = de;
  }

  protected Register de;

  public Ldi(Register de, RegisterPair bc, RegisterPair hl, Register flag, Memory memory, IO io, Register a) {
    super(bc, hl, flag, memory, io, ldiTableAluOperation);
    this.de = de;
    this.a = a;
  }

  public void execute() {
    int read = memory.read(hl.read(), 0);
    memory.write(de.read(), read);

    next();
    bc.decrement();

    flagOperation(read);
  }

  protected void flagOperation(int valueFromHL) {
    int byteTemp = valueFromHL + a.read();
    aluOperation.execute2Values1Boolean(flag.read(), byteTemp, bc.read() != 0 ? 1 : 0, flag);
  }

  protected void next() {
    hl.increment();
    de.increment();
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitLdi(this))
      super.accept(visitor);
  }
}

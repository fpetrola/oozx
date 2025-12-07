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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Cpi extends BlockInstruction {
  public static final AluOperation cpiTableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value, int A, int BC) {
      F = BC;
      int bytetemp = A - value;
      int lookup = ((A & 0x08) >> 3) |
                   ((value & 0x08) >> 2) |
                   ((bytetemp & 0x08) >> 1);
      F = (F & FLAG_C) | (BC != 0 ? (FLAG_V | FLAG_N) : FLAG_N) |
          halfCarrySubTable(lookup) | (bytetemp != 0 ? 0 : FLAG_Z) |
          (bytetemp & FLAG_S);
      if ((F & FLAG_H) != 0) bytetemp--;
      F |= (bytetemp & FLAG_3) | ((bytetemp & 0x02) != 0 ? FLAG_5 : 0);
      Q = F;
      return F;
    }
  };

  public Register getA() {
    return a;
  }

  public void setA(Register a) {
    this.a = a;
  }

  protected Register a;

  public Cpi(Register a, Register flag, RegisterPair bc, RegisterPair hl, Memory memory, IO io) {
    super(bc, hl, flag, memory, io);
    this.a = a;
  }

  public void execute() {
    bc.decrement();
    flagOperation(bc.read());
    next();
  }

  protected void flagOperation(int valueFromHL) {
    int value = memory.read(hl.read(), 0);
    int reg_A = a.read();
    cpiTableAluOperation.execute2Values1Boolean(value, reg_A, bc.read() != 0 ? 1 : 0, flag);
  }


  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitCpi(this))
      super.accept(visitor);
  }
}

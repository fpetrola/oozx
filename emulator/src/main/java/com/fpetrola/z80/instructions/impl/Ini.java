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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Ini extends BlockInstruction {
  public static final TableAluOperation iniTableAluOperation = new TableAluOperation() {
    protected int calculate3Values(int initemp2, int initemp, int B) {
      F = ((initemp & 0x80) != 0 ? FLAG_N : 0) |
          ((initemp2 < initemp) ? FLAG_H | FLAG_C : 0) |
          (parityTable((initemp2 & 0x07) ^ B) != 0 ? FLAG_P : 0) |
          sz53Table(B);
      Q = F;
      return F;
    }
  };

  public Ini(RegisterPair bc, RegisterPair hl, Register flag, Memory memory, IO io) {
    super(bc, hl, flag, memory, io);
  }

  public void execute() {
    int port = bc.read();
    int in = io.in(port);
    int cValue = bc.getLow().read();
    int hlValue = hl.read();
    memory.write(hlValue, in);
    next();
    bc.getHigh().decrement();
    flagOperation(in);
  }

  protected void flagOperation(int value) {
    int b = bc.getHigh().read();
    Register c = bc.getLow();
    int C = c.read() & 0xff;
    int B = b & 0xff;
    int i = getDirection();

    int initemp = value & 0xff;
    int initemp2 = (initemp + C + i) & 0xff;
    iniTableAluOperation.execute3Values(initemp, initemp2, B, flag);
  }

  protected int getDirection() {
    return 1;
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitIni(this))
      super.accept(visitor);
  }
}

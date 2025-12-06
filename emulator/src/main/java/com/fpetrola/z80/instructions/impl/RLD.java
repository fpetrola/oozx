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
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RLD extends AbstractInstruction {
  public static final TableAluOperation rldTableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int flag) {
      F = flag;
      A = (A & 0xf0) | (value >> 4);
      F = (F & FLAG_C) | sz53pTable(A);
      Q = F;
      return A;
    }
  };
  protected final Register a;

  public Register getHl() {
    return hl;
  }

  protected final Register hl;
  protected final Register flag;
  protected final Register r;
  protected final Memory memory;

  public RLD(Register a, Register hl, Register flag, Register r, Memory memory) {
    this.a = a;
    this.hl = hl;
    this.flag = flag;
    this.r = r;
    this.memory = memory;
  }

  public void execute() {
    int reg_A = a.read();
    int nibble1 = (reg_A & 0x00F0) >> 4;
    int nibble2 = reg_A & 0x000F;

    int temp = memory.read(hl.read(), 0);
    int nibble3 = (temp & 0x00F0) >> 4;
    int nibble4 = temp & 0x000F;

    memory.write(hl.read(), getTemp1(nibble2, nibble3, nibble4));
    int value = getRegA1(nibble1, nibble4, nibble3);

    executeAlu(temp, reg_A);

    a.write(value);
  }

  protected void executeAlu(int value, int reg_A) {
    rldTableAluOperation.execute1ValueAndCarry(value, reg_A, flag);
  }

  protected int getTemp1(int nibble2, int nibble3, int nibble4) {
    return (nibble4 << 4) | nibble2;
  }

  protected int getRegA1(int nibble1, int nibble4, int nibble3) {
    return (nibble1 << 4) | nibble3;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitRLD(this)) {
      super.accept(visitor);
    }
  }
}

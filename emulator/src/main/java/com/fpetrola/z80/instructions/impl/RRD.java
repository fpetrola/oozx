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
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RRD extends RLD {
  public static final TableAluOperation rrdTableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value1, int value2, int flag) {
      value1 = (value1 & 0xf0) | (value2 & 0x0f);
      F = (F & FLAG_C) | sz53pTable(value1);
      Q = F;
      return value1;
    }
  };

  public RRD(Register a, Register hl, Register r, Register flag, Memory memory) {
    super(a, hl, flag, r, memory);
  }

  protected void executeAlu(int value, int reg_A) {
    rrdTableAluOperation.execute2ValuesAndCarry(value, reg_A, flag);
  }

  protected int getTemp1(int nibble2, int nibble3, int nibble4) {
    return (nibble2 << 4) | nibble3;
  }

  protected int getRegA1(int nibble1, int nibble4, int nibble3) {
    return (nibble1 << 4) | nibble4;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitRRD(this)) {
      super.accept(visitor);
    }
  }
}

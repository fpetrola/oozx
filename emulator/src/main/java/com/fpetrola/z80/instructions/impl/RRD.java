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
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RRD<T extends WordNumber> extends RLD<T> {
  public static final TableAluOperation rrdTableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int flag) {
      A = (A & 0xf0) | (value & 0x0f);
      F = (F & FLAG_C) | sz53p_table[A];
      Q = F;
      return A;
    }
  };

  public RRD(Register<T> a, Register<T> hl, Register<T> r, Register<T> flag, Memory<T> memory) {
    super(a, hl, flag, r, memory);
  }

  protected void executeAlu(T value, T reg_A) {
    rrdTableAluOperation.executeWithCarry(value, reg_A, flag);
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

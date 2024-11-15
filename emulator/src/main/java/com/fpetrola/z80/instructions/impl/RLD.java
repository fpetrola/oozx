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
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class RLD<T extends WordNumber> extends AbstractInstruction<T> {
  public static final TableAluOperation rldTableAluOperation = new TableAluOperation() {
    public int execute(int a, int carry) {
      data = carry;
      if ((a & 0x80) == 0)
        resetS();
      else
        setS();
      setZ(a == 0);
      resetH();
      setPV(parity[a]);
      resetN();
      setUnusedFlags(a);
      return a;
    }
  };
  protected final Register<T> a;

  public Register<T> getHl() {
    return hl;
  }

  protected final Register<T> hl;
  protected final Register<T> flag;
  protected final Register<T> r;
  protected final Memory<T> memory;

  public RLD(Register<T> a, Register<T> hl, Register<T> flag, Register<T> r, Memory<T> memory) {
    this.a = a;
    this.hl = hl;
    this.flag = flag;
    this.r = r;
    this.memory = memory;
  }

  public int execute() {
    int reg_A = a.read().intValue();
    int nibble1 = (reg_A & 0x00F0) >> 4;
    int nibble2 = reg_A & 0x000F;

    int temp = memory.read(hl.read()).intValue();
    int nibble3 = (temp & 0x00F0) >> 4;
    int nibble4 = temp & 0x000F;

    memory.write(hl.read(), createValue(getTemp1(nibble2, nibble3, nibble4)));
    T value = createValue(getRegA1(nibble1, nibble4, nibble3));

    executeAlu(value);

    a.write(value);

    return 1;
  }

  protected void executeAlu(T value) {
    rldTableAluOperation.executeWithCarry(value, flag);
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

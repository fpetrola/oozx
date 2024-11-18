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

import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Cpi<T extends WordNumber> extends BlockInstruction<T> {
  public static final AluOperation cpiTableAluOperation = new TableAluOperation() {
    public int execute(int reg_A, int value, int carry) {
      data = carry;
      //    reg_R++;
      int result = reg_A - value;
      //
      if ((result & 0x0080) == 0)
        resetS();
      else
        setS();
      result = result & 0x00FF;
      if (result == 0)
        setZ();
      else
        resetZ();
      setHalfCarryFlagSub(reg_A, value);
      setPV(carry == 1);
      setN();
      if (getH())
        value--;
      set5((result & 0x00002) != 0);
      set3((result & 0x00008) != 0);

      return reg_A;
    }
  };

  public Register<T> getA() {
    return a;
  }

  public void setA(Register<T> a) {
    this.a = a;
  }

  protected Register<T> a;

  public Cpi(Register<T> a, Register<T> flag, RegisterPair<T> bc, RegisterPair<T> hl, Memory<T> memory, IO<T> io) {
    super(bc, hl, flag, memory, io);
    this.a = a;
  }

  public int execute() {
    memory.disableReadListener();
    memory.disableWriteListener();
    bc.decrement();
    flagOperation(bc.read());
    next();
    memory.enableReadListener();
    memory.enableWriteListener();
    return 1;
  }

  protected void flagOperation(T valueFromHL) {
    T value = memory.read(hl.read(), 0);
    T reg_A = a.read();
    cpiTableAluOperation.executeWithCarry2(value, reg_A, bc.read().isNotZero() ? 1 : 0, flag);
  }


  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitCpi(this);
  }
}

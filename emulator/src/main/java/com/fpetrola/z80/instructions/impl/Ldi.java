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

public class Ldi<T extends WordNumber> extends BlockInstruction<T> {
  public static final AluOperation ldiTableAluOperation = new TableAluOperation() {
    public int execute(int bc, int data1, int carry) {
      data = bc;
      resetH();
      resetN();
      setPV(carry != 0);
      setUnusedFlags(data1);
      return data;
    }
  };
  protected final Register<T> a;

  public Register<T> getDe() {
    return de;
  }

  public void setDe(Register<T> de) {
    this.de = de;
  }

  protected Register<T> de;

  public Ldi(Register<T> de, RegisterPair<T> bc, RegisterPair<T> hl, Register<T> flag, Memory<T> memory, IO<T> io, Register<T> a) {
    super(bc, hl, flag, memory, io);
    this.de = de;
    this.a= a;
  }

  public int execute() {
    memory.disableReadListener();
    memory.disableWriteListener();
    T read = memory.read(hl.read());
    memory.write(de.read(), read);

    next();
    bc.decrement();

    flagOperation(read);
    memory.enableReadListener();
    memory.enableWriteListener();

    return 1;
  }

  protected void flagOperation(T valueFromHL) {
    flag.write(Ldd.lddTableAluOperation.executeWithCarry2(valueFromHL, a.read(), bc.read().intValue() != 0 ? 1 : 0, flag));
  }

  protected void next() {
    hl.increment();
    de.increment();
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitLdi(this);
  }
}

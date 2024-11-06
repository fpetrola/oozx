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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.instructions.base.BlockInstruction;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.mmu.IO;
import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Ldi<T extends WordNumber> extends BlockInstruction<T> {
  public static final AluOperation ldiTableAluOperation = new AluOperation() {
    public int execute(int bc, int carry) {
      resetH();
      resetN();
      setPV(bc != 0);
      return data;
    }
  };

  public Register<T> getDe() {
    return de;
  }

  public void setDe(Register<T> de) {
    this.de = de;
  }

  protected Register<T> de;

  public Ldi(Register<T> de, RegisterPair<T> bc, Register<T> hl, Register<T> flag, Memory<T> memory, IO<T> io) {
    super(bc, hl, flag, memory, io);
    this.de = de;
  }

  public int execute() {
    memory.disableReadListener();
    memory.disableWriteListener();
    memory.write(de.read(), memory.read(hl.read()));

    next();
    bc.decrement();

    flagOperation();
    memory.enableReadListener();
    memory.enableWriteListener();

    return 1;
  }

  protected void flagOperation() {
    ldiTableAluOperation.executeWithCarry(bc.read(), flag);
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

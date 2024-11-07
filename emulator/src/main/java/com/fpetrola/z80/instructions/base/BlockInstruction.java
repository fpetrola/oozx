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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

public abstract class BlockInstruction<T extends WordNumber> extends AbstractInstruction<T> {
  public RegisterPair<T> getBc() {
    return bc;
  }

  public void setBc(RegisterPair<T> bc) {
    this.bc = bc;
  }

  public Register<T> getHl() {
    return hl;
  }

  public void setHl(Register<T> hl) {
    this.hl = hl;
  }

  public Register<T> getFlag() {
    return flag;
  }

  public void setFlag(Register<T> flag) {
    this.flag = flag;
  }

  public Memory<T> getMemory() {
    return memory;
  }

  public void setMemory(Memory<T> memory) {
    this.memory = memory;
  }

  public IO<T> getIo() {
    return io;
  }

  public void setIo(IO<T> io) {
    this.io = io;
  }

  protected RegisterPair<T> bc;
  protected Register<T> hl;
  protected Register<T> flag;
  protected Memory<T> memory;
  protected IO<T> io;

  public BlockInstruction(RegisterPair<T> bc, Register<T> hl, Register<T> flag, Memory<T> memory, IO<T> io) {
    this.bc = bc;
    this.hl = hl;
    this.flag = flag;
    this.memory = memory;
    this.io = io;
  }

  protected abstract void flagOperation();

  protected void next() {
    hl.increment();
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitBlockInstruction(this);
  }
}

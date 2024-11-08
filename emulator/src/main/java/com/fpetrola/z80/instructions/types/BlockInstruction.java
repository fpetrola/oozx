/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
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

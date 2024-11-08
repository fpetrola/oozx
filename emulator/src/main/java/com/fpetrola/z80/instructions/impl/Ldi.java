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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;

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

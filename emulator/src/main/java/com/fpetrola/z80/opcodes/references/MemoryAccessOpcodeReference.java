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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;

public class MemoryAccessOpcodeReference<T extends WordNumber> implements OpcodeReference<T> {
  public ImmutableOpcodeReference<T> getC() {
    return c;
  }

  private final ImmutableOpcodeReference<T> c;
  private Memory<T> mem;

  public MemoryAccessOpcodeReference(ImmutableOpcodeReference<T> c, Memory mem) {
    this.c = c;
    this.mem = mem;
  }

  @Override
  public T read() {
    return mem.read(c.read());
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void write(T value) {
    this.mem.write(c.read(), value);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new MemoryAccessOpcodeReference<>(c, mem);
  }

  @Override
  public String toString() {
    return "[" + c + "]";
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitMemoryAccessOpcodeReference(this);
  }
}

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

import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Out<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public Out(ImmutableOpcodeReference source, OutPortOpcodeReference outPortOpcodeReference, Register<T> flag) {
    super(outPortOpcodeReference, source, flag);
  }

  public int execute() {
    target.write(source.read());
    return cyclesCost;
  }

  public static class OutPortOpcodeReference<T> implements OpcodeReference<T> {
    private final IO<T> io;
    private final ImmutableOpcodeReference target;

    public OutPortOpcodeReference(IO<T> io, ImmutableOpcodeReference target) {
      this.io = io;
      this.target = target;
    }

    public void write(T value) {
      io.out((T) target.read(), value);
    }

    public T read() {
      return (T) target.read();
    }

    public int getLength() {
      return target.getLength();
    }

    public Object clone() throws CloneNotSupportedException {
      return target.clone();
    }

    public String toString() {
      return target.toString();
    }
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitOut(this);
  }
}

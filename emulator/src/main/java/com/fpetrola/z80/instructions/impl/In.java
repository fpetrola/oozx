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
import com.fpetrola.z80.opcodes.references.MutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class In<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public static AluOperation inCTableAluOperation = new AluOperation() {
    public int execute(int a, int carry) {
      if ((a & 0x0080) == 0)
        resetS();
      else
        setS();
      if (a == 0)
        setZ();
      else
        resetZ();
      if (parity[a & 0xff])
        setPV();
      else
        resetPV();
      resetN();
      resetH();
      return a;
    }
  };

  public ImmutableOpcodeReference<T> getA() {
    return a;
  }

  public void setA(ImmutableOpcodeReference<T> a) {
    this.a = a;
  }

  public ImmutableOpcodeReference<T> getBc() {
    return bc;
  }

  public void setBc(ImmutableOpcodeReference<T> bc) {
    this.bc = bc;
  }

  private ImmutableOpcodeReference<T> a;
  private ImmutableOpcodeReference<T> bc;
  private MutableOpcodeReference<T> memptr;
  private IO<T> io;

  public In(OpcodeReference target, ImmutableOpcodeReference source, ImmutableOpcodeReference<T> a, ImmutableOpcodeReference<T> bc, Register<T> flag, MutableOpcodeReference<T> memptr, IO<T> io) {
    super(target, source, flag);
    this.a = a;
    this.bc = bc;
    this.memptr = memptr;
    this.io = io;
  }

  public int execute() {
    T port = source.read();

    boolean equalsN = !(source instanceof Register);
    if (equalsN) {
      port = port.or(a.read().left(8));
      memptr.write(port.plus1());
    } else {
      port = bc.read();
    }

    T value = io.in(port);

    target.write(value);

    if (!equalsN)
      inCTableAluOperation.executeWithCarry(value, flag);
    else
      flag.write(flag.read());

    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitIn(this);
  }
}

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
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class In<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public static AluOperation inCTableAluOperation = new TableAluOperation() {
    public int execute(int value, int reg, int carry) {
      F = value;
      F = ( F & FLAG_C) | sz53pTable[(reg)];
      Q = F;
      return reg;
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
  private final IO<T> io;

  public In(OpcodeReference target, ImmutableOpcodeReference source, ImmutableOpcodeReference<T> a, ImmutableOpcodeReference<T> bc, Register<T> flag, IO<T> io) {
    super(target, source, flag);
    this.a = a;
    this.bc = bc;
    this.io = io;
  }

  public int execute() {
    T port = source.read();

    boolean equalsN = !(source instanceof Register);
    if (equalsN) {
      port = port.or(a.read().left(8));
    } else {
      port = bc.read();
    }

    T value = io.in(port);

    target.write(value);

    if (!equalsN)
      inCTableAluOperation.executeWithCarry(value, flag.read(), flag);
    else
      flag.write(flag.read());

    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitIn(this);
  }
}

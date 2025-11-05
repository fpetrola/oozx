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
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class In extends TargetSourceInstruction<ImmutableOpcodeReference> {
  public final static AluOperation inCTableAluOperation = new TableAluOperation() {
    public int execute(int value, int reg, int carry) {
      F = value;
      F = (F & FLAG_C) | sz53pTable((reg));
      Q = F;
      return reg;
    }
  };

  public ImmutableOpcodeReference getA() {
    return a;
  }

  public void setA(ImmutableOpcodeReference a) {
    this.a = a;
  }

  public ImmutableOpcodeReference getBc() {
    return bc;
  }

  public void setBc(ImmutableOpcodeReference bc) {
    this.bc = bc;
  }

  private ImmutableOpcodeReference a;
  private ImmutableOpcodeReference bc;
  private final IO io;

  public In(OpcodeReference target, ImmutableOpcodeReference source, ImmutableOpcodeReference a, ImmutableOpcodeReference bc, Register flag, IO io) {
    super(target, source, flag);
    this.a = a;
    this.bc = bc;
    this.io = io;
  }

  public int execute() {
    int port = source.read();

    boolean equalsN = !(source instanceof Register);
    if (equalsN) {
      port = (port | a.read() << 8) & 0xFFFF;
    } else {
      port = bc.read();
    }

    int value = io.in(port);

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

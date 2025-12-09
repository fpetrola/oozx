/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

public class In extends TargetSourceInstruction<ImmutableOpcodeReference> {
  public final static AluOperation inTableAluOperation = new AluOperation() {
    protected int calculate2Values1Boolean(int value1, int value2, int carry) {
      F = value2;
      F = (F & FLAG_C) | sz53pTable((value1));
      Q = F;
      return value1;
    }
  };

  private final ImmutableOpcodeReference a;
  private final ImmutableOpcodeReference bc;
  private final IO io;
  private final boolean notRegister;

  public In(OpcodeReference target, ImmutableOpcodeReference source, ImmutableOpcodeReference a, ImmutableOpcodeReference bc, Register flag, IO io) {
    super(target, source, flag, inTableAluOperation);
    this.a = a;
    this.bc = bc;
    this.io = io;
    notRegister = !(this.source instanceof Register);
  }

  public void execute() {
    int port = source.read();
    port = notRegister ? (port | a.read() << 8) & 0xFFFF : bc.read();
    int value = io.in(port);
    target.write(value);
    if (!notRegister)
      aluOperation.execute2ValuesAndCarry(value, flag.read(), flag);
  }

  public ImmutableOpcodeReference getA() {
    return a;
  }

  public ImmutableOpcodeReference getBc() {
    return bc;
  }

  public void accept(InstructionVisitor<?> visitor) {
    super.accept(visitor);
    visitor.visitIn(this);
  }
}

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
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class LdAR extends Ld {
  public static final TableAluOperation ldarTableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value1, int value2, int IFF2) {
      F = value2;
      int A1 = value1 & 0xff;
      F = (F & FLAG_C) | sz53Table(A1) | (IFF2 != 0 ? FLAG_V : 0);
      Q = F;
      return F;
    }
  };
  private final State state;

  public LdAR(OpcodeReference target, ImmutableOpcodeReference source, Register flag, State state) {
    super(target, source, flag);
    this.state = state;
  }

  public void execute() {
    int value = source.read();
    int i = ldarTableAluOperation.execute2Values1Boolean(value, flag.read(), state.isIff2() ? 1 : 0, flag);
    flag.write(i);
    target.write(value);
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitLdAR(this)) {
      super.accept(visitor);
    }
  }
}

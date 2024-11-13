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
import com.fpetrola.z80.registers.flag.AluOperation;

public class LdAR extends Ld {
  /**
   * The flags LD A,R leaves: sign and zero from the value loaded, carry kept, half-carry and
   * subtract cleared, and parity/overflow from IFF2.
   * <p>
   * This had the two the wrong way round - it took sign and zero from the FLAGS register and
   * the carry from R - so the flags came out the same whatever R held. Sign in particular was
   * never set, and a game that reads R and branches on its top bit never branches: Ping Pong
   * plays a tone in a loop it leaves with LD A,R / RET M, and the tone never ended.
   * <p>
   * F arrives holding the real flags, put there by the caller, so it is not assigned here - the
   * assignment that was here is what threw them away. LD A,I next door has always done it this
   * way.
   */
  public static class LdarTableAluOperation extends AluOperation {
    @Override
    protected int calculate2Values1Boolean(int loaded, int unusedFlags, int IFF2) {
      F = (F & FLAG_C) | sz53Table(loaded & 0xff) | (IFF2 != 0 ? FLAG_V : 0);
      Q = F;
      return F;
    }
  }
  private final State state;

  public LdAR(OpcodeReference target, ImmutableOpcodeReference source, Register flag, State state) {
    super(target, source, flag, new LdarTableAluOperation());
    this.state = state;
  }

  public void execute() {
    int value = source.read();
    int i = aluOperation.execute2Values1Boolean(value, flag.read(), state.isIff2() ? 1 : 0, flag);
    flag.write(i);
    target.write(value);
  }

  @Override
  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitLdAR(this)) {
      super.accept(visitor);
    }
  }
}

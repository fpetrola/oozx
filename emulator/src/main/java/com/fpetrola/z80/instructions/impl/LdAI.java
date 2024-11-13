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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class LdAI extends Ld {
  /**
   * LD A,I sets sign/zero from the loaded value, clears half-carry/subtract, sets P/V from IFF2
   * and keeps carry. The shared AluOperation table seeds F from its second argument, so the
   * flags register must be passed second and the loaded value first.
   */
  public static class LdaiTableAluOperation extends AluOperation {
    @Override
    protected int calculate2Values1Boolean(int loaded, int flagsIn, int IFF2) {
      F = (F & FLAG_C) | sz53Table(loaded & 0xff) | (IFF2 != 0 ? FLAG_V : 0);
      Q = F;
      return F;
    }
  }
  private final State state;

  public LdAI(OpcodeReference target, OpcodeReference source, Register flag, State state) {
    super(target, source, flag, new LdaiTableAluOperation());
    this.state = state;
  }

  public void execute() {
    int value = source.read();
    boolean iff2 = state.isIff2();
    aluOperation.execute2Values1Boolean(value, flag.read(), iff2 ? 1 : 0, flag);

    target.write(value);


  }
}

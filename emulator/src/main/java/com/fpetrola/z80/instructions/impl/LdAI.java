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
   * The flags LD A,I leaves, which are LD A,R's: sign and zero from the value loaded, carry
   * kept, half-carry and subtract cleared, and parity/overflow from IFF2.
   * <p>
   * The order of the two arguments is not free. Every operation here runs through a table built
   * once over all of them, and that table seeds F from the SECOND - so the second has to be the
   * flags, and the first the value. This was handed them the other way round, so the carry came
   * from bit 0 of I rather than from the carry, wrong in half of all cases.
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

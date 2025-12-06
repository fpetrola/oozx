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
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class LdAI extends Ld {
  public static final AluOperation ldaiTableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value1, int value2, int IFF2) {
      value2 = value1;
      F = (F & FLAG_C) | sz53Table(value2) | (IFF2 != 0 ? FLAG_V : 0);
      Q = F;
      return F;
    }
  };
  private final State state;

  public LdAI(OpcodeReference target, OpcodeReference source, Register flag, State state) {
    super(target, source, flag);
    this.state = state;
  }

  public void execute() {
    int value = source.read();
    int reg_A = target.read();
    boolean iff2 = state.isIff2();
    int ldar = ldaiTableAluOperation.execute2Values1Boolean(reg_A, value, iff2 ? 1 : 0, flag);

    target.write(value);

    
  }
}

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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class LdAR<T extends WordNumber> extends Ld<T> {
  public static final AluOperation ldarTableAluOperation = new TableAluOperation() {
    public int execute(int reg_R, int reg_A, int carry) {
      reg_A = reg_R & 0x7F;
      setS((reg_A & FLAG_S) != 0);
      setZ(reg_A == 0);
      resetH();
      resetN();
      setPV(carry == 1);

      return reg_A;
    }
  };
  private final State<T> state;

  public LdAR(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag, State<T> state) {
    super(target, source, flag);
    this.state = state;
  }

  public int execute() {
    T value = source.read();
    T reg_A = target.read();
    boolean iff2 = state.isIff2();
    T ldar = ldarTableAluOperation.executeWithCarry2(reg_A, value, iff2 ? 1 : 0, flag);

    target.write(ldar);

    return cyclesCost;
  }
}

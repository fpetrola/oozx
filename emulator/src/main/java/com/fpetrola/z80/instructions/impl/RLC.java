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
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class RLC<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {

  public static final TableAluOperation rlcTableAluOperation1 = new TableAluOperation() {
    public int execute(int a, int carry) {
      data = carry;

      a = a << 1;
      if ((a & 0x0FF00) != 0) {
        setC();
        a = a | 0x01;
      } else
        resetC();
      // standard flag updates
      if ((a & FLAG_S) == 0)
        resetS();
      else
        setS();
      if ((a & 0x00FF) == 0)
        setZ();
      else
        resetZ();
      resetH();
      resetN();
      // put value back
      a = a & 0x00FF;
      setPV(parity[a]);
      setUnusedFlags(a);

      return a;
    }
  };

  public RLC(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, temp1) -> rlcTableAluOperation1.executeWithCarry(temp1, tFlagRegister));
  }

  public int execute() {
    final T value2 = target.read();
    T execute = unaryAluOperation.execute(flag, value2);
    target.write(execute);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingRlc(this))
      super.accept(visitor);
  }
}

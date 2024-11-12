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

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Ind<T extends WordNumber> extends Ini<T> {
  public static final AluOperation indTableAluOperation = new AluOperation() {
    public <T extends WordNumber> T executeWithCarry(T value, T b, Register<T> c) {
      int hlMem = value.intValue() & 0xff;
      int regB = b.intValue() & 0xff;
      regB = decAndSetFlags(regB);
      var incC = (c.read().intValue() - 1) & 255;
      setH(hlMem + incC > 255);
      setC(hlMem + incC > 255);
      setPV(isEvenParity((((hlMem + incC) & 7) ^ regB)));
      setN((hlMem & 0x80) != 0);

      return WordNumber.createValue(data);
    }
  };


  public Ind(RegisterPair<T> bc, RegisterPair<T> hl, Register<T> flag, Memory<T> memory, IO<T> io) {
    super(bc, hl, flag, memory, io);
  }

  protected void next() {
    hl.decrement();
  }

  protected void flagOperation(T valueFromHL) {
    T t = indTableAluOperation.executeWithCarry(valueFromHL, bc.getHigh().read(), bc.getLow());
    flag.write(t);
  }
}

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

import com.fpetrola.z80.instructions.base.BlockInstruction;
import com.fpetrola.z80.mmu.IO;
import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Ini<T extends WordNumber> extends BlockInstruction<T> {
  public static final TableAluOperation iniTableAluOperation = new TableAluOperation() {
    public int execute(int b, int carry) {
      data = 0;
      setZ(b == 0);
      setN();
      return b;
    }
  };

  public Ini(RegisterPair<T> bc, Register<T> hl, Register<T> flag, Memory<T> memory, IO<T> io) {
    super(bc, hl, flag, memory, io);
  }

  public int execute() {
    T hlValue = hl.read();
    T cValue = bc.getLow().read();
    T in = io.in(cValue);
    memory.write(hlValue, in);
    bc.getHigh().decrement();
    next();
    flagOperation();
    return 1;
  }

  protected void flagOperation() {
    iniTableAluOperation.executeWithCarry(bc.getHigh().read(), flag);
  }
}

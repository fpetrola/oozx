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
import com.fpetrola.z80.instructions.types.BlockInstruction;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Outi<T extends WordNumber> extends BlockInstruction<T> {
  public static final AluOperation outiTableAluOperation = new TableAluOperation() {
    public <T extends WordNumber> T executeWithCarry(T value, T b, Register<T> l) {
      int work8 = value.intValue() & 0xff;
      int regB = b.intValue() & 0xff;
      int regL = l.read().intValue() & 0xFF;
      regB = decAndSetFlags(regB);
      setH(work8 + regL > 255);
      setC(work8 + regL > 255);
      setPV(isEvenParity(((work8 + regL) & 7) ^ regB));
      setN((work8 & 0x80) != 0);
      return WordNumber.createValue(data);
    }
  };

  public Outi(RegisterPair<T> bc, RegisterPair<T> hl, Register<T> flag, Memory<T> memory, IO<T> io) {
    super(bc, hl, flag, memory, io);
  }

  public int execute() {
    T hlValue = hl.read();
    T cValue = bc.getLow().read();
    T valueFromHL = memory.read(hlValue, 0);
    bc.getHigh().decrement();
    io.out(bc.read(), valueFromHL);
    next();
    flagOperation(valueFromHL);

    return 1;
  }

  protected void flagOperation(T valueFromHL) {
    T t = outiTableAluOperation.executeWithCarry(valueFromHL, bc.getHigh().read(), hl.getLow());
    flag.write(t);
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitOuti(this))
      super.accept(visitor);
  }
}

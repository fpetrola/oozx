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
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import com.fpetrola.z80.registers.flag.AluOperation;

public class Cpd<T extends WordNumber> extends Cpi<T> {
  public static final AluOperation cpdTableAluOperation = new AluOperation() {
    public int execute(int reg_A, int value, int carry) {
      doCPD(reg_A, value, carry == 1);
      return reg_A;
    }
  };

  public Cpd(Register<T> a, Register flag, RegisterPair<T> bc, RegisterPair<T> hl, Memory<T> memory, IO<T> io) {
    super(a, flag, bc, hl, memory, io);
  }

  protected void next() {
    hl.decrement();
  }

  protected void flagOperation(T valueFromHL) {
    T value = memory.read(hl.read());
    T reg_A = a.read();
    cpdTableAluOperation.executeWithCarry2(value, reg_A, bc.read().isNotZero() ? 1 : 0, flag);
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitCpd(this))
      super.accept(visitor);
  }
}

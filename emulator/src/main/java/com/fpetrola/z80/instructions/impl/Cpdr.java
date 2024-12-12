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
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Flags;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

public class Cpdr<T extends WordNumber> extends RepeatingInstruction<T> {
  private final Register<T> flag;

  public Cpdr(ImmutableOpcodeReference<T> pc, RegisterPair<T> bc, Register<T> flag, Cpd cpd) {
    super(cpd, pc, bc);
    this.flag = flag;
  }

  protected boolean checkLoopCondition() {
    return (flag.read().intValue() & Flags.ZERO_FLAG) == 0 && bc.read().isNotZero();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitCpdr(this))
      super.accept(visitor);
  }
}

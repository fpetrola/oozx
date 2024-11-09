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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class RST<T extends WordNumber> extends AbstractInstruction<T>  implements JumpInstruction<T> {
  private final int p;
  private final ImmutableOpcodeReference<T> pc;
  private final Register<T> sp;
  private final Memory<T> memory;

  public RST(int p, ImmutableOpcodeReference<T> pc, Register<T> sp, Memory<T> memory) {
    this.p = p;
    this.pc = pc;
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    Push.doPush(pc.read().plus1(), sp, memory);
    setNextPC(WordNumber.createValue(p & 0xFFFF));
    return 5 + 3 + 3;
  }

  public String toString() {
    return "RST " + String.format("%02X", p);
  }

  public int getP() {
    return p;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingRst(this);
  }
}

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
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;

public class RST extends AbstractInstruction implements JumpInstruction {
  private final int p;
  private final ImmutableOpcodeReference pc;
  private final Register sp;
  private final Memory memory;

  public RST(int p, ImmutableOpcodeReference pc, Register sp, Memory memory) {
    this.p = p;
    this.pc = pc;
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    Integer wordNumber = pc.read();
    Push.doPush((wordNumber + 1) & 0xFFFF, sp, memory);
    setNextPC(p);
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

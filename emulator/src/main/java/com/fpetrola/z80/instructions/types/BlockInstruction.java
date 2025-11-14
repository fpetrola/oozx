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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;

public abstract class BlockInstruction extends AbstractInstruction {
  final protected RegisterPair bc;
  final protected RegisterPair hl;
  final protected Register flag;
  final protected Memory memory;
  final protected IO io;

  public BlockInstruction(RegisterPair bc, RegisterPair hl, Register flag, Memory memory, IO io) {
    this.bc = bc;
    this.hl = hl;
    this.flag = flag;
    this.memory = memory;
    this.io = io;
  }

  protected abstract void flagOperation(int valueFromHL);

  protected void next() {
    hl.increment();
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitBlockInstruction(this);
  }

  public RegisterPair getBc() {
    return bc;
  }

  public RegisterPair getHl() {
    return hl;
  }

  public Register getFlag() {
    return flag;
  }

  public Memory getMemory() {
    return memory;
  }

  public IO getIo() {
    return io;
  }
}

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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;

public class MemoryAccessOpcodeReference implements OpcodeReference {
  public ImmutableOpcodeReference getC() {
    return c;
  }

  private final ImmutableOpcodeReference c;
  private final Memory mem;

  public MemoryAccessOpcodeReference(ImmutableOpcodeReference c, Memory mem) {
    this.c = c;
    this.mem = mem;
  }

  @Override
  public int read() {
    return mem.read(c.read(), 0);
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void write(int value) {
    this.mem.write(c.read(), value);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new MemoryAccessOpcodeReference(c, mem);
  }

  @Override
  public String toString() {
    return "[" + c + "]";
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitMemoryAccessOpcodeReference(this);
  }
}

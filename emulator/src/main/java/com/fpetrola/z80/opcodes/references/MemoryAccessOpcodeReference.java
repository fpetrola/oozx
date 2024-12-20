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

package com.fpetrola.z80.opcodes.references;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;

public class MemoryAccessOpcodeReference<T extends WordNumber> implements OpcodeReference<T> {
  public ImmutableOpcodeReference<T> getC() {
    return c;
  }

  private final ImmutableOpcodeReference<T> c;
  private final Memory<T> mem;

  public MemoryAccessOpcodeReference(ImmutableOpcodeReference<T> c, Memory mem) {
    this.c = c;
    this.mem = mem;
  }

  @Override
  public T read() {
    return mem.read(c.read(), 0);
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void write(T value) {
    this.mem.write(c.read(), value);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new MemoryAccessOpcodeReference<>(c, mem);
  }

  @Override
  public String toString() {
    return "[" + c + "]";
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitMemoryAccessOpcodeReference(this);
  }
}

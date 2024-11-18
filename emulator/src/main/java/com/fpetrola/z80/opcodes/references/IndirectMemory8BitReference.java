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

public final class IndirectMemory8BitReference<T> implements OpcodeReference<T> {

  public ImmutableOpcodeReference<T> target;
  public T address;
  public T value;

  public Memory<T> getMemory() {
    return memory;
  }

  private final Memory<T> memory;

  public IndirectMemory8BitReference(ImmutableOpcodeReference target, Memory memory) {
    this.target = target;
    this.memory = memory;
  }

  public T read() {
    address = target.read();
    value = memory.read(address, 0);
    return value;
  }

  public void write(T value) {
    address = target.read();
    memory.write(address, value);
  }

  public String toString() {
    return "(" + target + ")";
  }

  public int getLength() {
    return target.getLength();
  }

  public ImmutableOpcodeReference<T> getTarget() {
    return target;
  }

  public Object clone() throws CloneNotSupportedException {
    return new IndirectMemory8BitReference((ImmutableOpcodeReference) target.clone(), memory);
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitIndirectMemory8BitReference(this);
  }
}

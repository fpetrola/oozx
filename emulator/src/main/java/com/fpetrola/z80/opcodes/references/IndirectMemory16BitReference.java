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
import com.fpetrola.z80.registers.Register;

import java.util.function.BiConsumer;

public final class IndirectMemory16BitReference implements OpcodeReference {
  private final BiConsumer<Integer, Integer> memoryWriter;
  public ImmutableOpcodeReference target;
  public int address;

  public Memory getMemory() {
    return memory;
  }

  private final Memory memory;

  public IndirectMemory16BitReference(ImmutableOpcodeReference target, Memory memory) {
    this.target = target;
    this.memory = memory;

    if (target instanceof Register register && register.getName().equals("SP"))
      memoryWriter = (value, address) -> Memory.write16Bits(memory, value, address);
    else
      memoryWriter = (value, address) -> Memory.write16BitsR(memory, value, address);
  }

  public int read() {
    address = target.read();
    int fetchAddress = Memory.read16Bits(memory, address);
    return fetchAddress;
  }

  public void write(int value) {
    address = target.read();
    memoryWriter.accept(value, address);
  }

  public String toString() {
    return "(" + target.toString() + ")";
  }

  public int getLength() {
    return target.getLength();
  }

  public Object clone() throws CloneNotSupportedException {
    return new IndirectMemory16BitReference((ImmutableOpcodeReference) target.clone(), memory);
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitIndirectMemory16BitReference(this);
  }
}

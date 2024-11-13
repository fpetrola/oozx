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
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.PrimitiveIntBiFunction;

public final class IndirectMemory16BitReference implements OpcodeReference {
  private final PrimitiveIntBiFunction memoryWriter;
  private final ImmutableOpcodeReference target;
  private final Memory memory;
  public int address;

  public IndirectMemory16BitReference(ImmutableOpcodeReference target, Memory memory) {
    this.target = target;
    this.memory = memory;

    if (target instanceof Register register && register.getName().equals("SP"))
      memoryWriter = memory::write16Bits;
    else
      memoryWriter = memory::write16BitsReverse;
  }

  public int read() {
    address = target.read();
    return memory.read16Bits(address);
  }

  public void write(int value) {
    address = target.read();
    memoryWriter.applyAsInt(value, address);
  }

  public Memory getMemory() {
    return memory;
  }

  public int getLength() {
    return target.getLength();
  }

  public ImmutableOpcodeReference getTarget() {
    return target;
  }

  public String toString() {
    return "(" + target.toString() + ")";
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitIndirectMemory16BitReference(this);
  }
}

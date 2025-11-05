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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.memory.Memory;

public class Memory16BitReference implements OpcodeReference {
  private final Memory memory;
  private final ImmutableOpcodeReference pc;
  private final int delta;

  public Memory getMemory() {
    return memory;
  }

  public ImmutableOpcodeReference getPc() {
    return pc;
  }

  public int getDelta() {
    return delta;
  }

  public Memory16BitReference(Memory memory, ImmutableOpcodeReference pc, int delta) {
    this.memory = memory;
    this.pc = pc;
    this.delta = delta;
  }

  public int read() {
    return Memory.read16Bits(memory, (pc.read() + delta) & 0xFFFF);
  }

  public void write(int value) {
    int address = Memory.read16Bits(memory, (pc.read() + delta) & 0xFFFF);
    Memory.write16Bits(memory, value, address);
  }

  public String toString() {
    Integer read = 1;
    if (read == null) {
      return "";
    } else {
      return "0x" + Helper.formatAddress(read);
    }
  }

  public int getLength() {
    return 2;
  }

  public void accept(InstructionVisitor instructionVisitor) {
    if (!instructionVisitor.visitMemory16BitReference(this))
      OpcodeReference.super.accept(instructionVisitor);
  }

  public Object clone() throws CloneNotSupportedException {
    int lastFetchedAddress = 1;
    return new CachedMemory16BitReference(lastFetchedAddress, memory, pc, delta);
  }

}

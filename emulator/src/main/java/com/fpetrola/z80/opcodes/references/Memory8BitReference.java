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

public class Memory8BitReference implements ImmutableOpcodeReference {
  private final Memory memory;
  private final int delta;
  private final Register pc;

  public Memory8BitReference(Memory memory, Register pc, int delta) {
    this.memory = memory;
    this.pc = pc;
    this.delta = delta;
  }

  final public int read() {
    return memory.read((pc.read() + delta) & 0xFFFF, 0);
  }

  final public void write(int value) {
    memory.write(pc.read(), value);
  }

  public String toString() {
    Integer read = 1;
    //return read == null ? "" : "0x" + Helper.convertToHex(read.intValue()) + "";
    if (read == null) {
      return "";
    } else {
      return read + "";
    }
  }

  public int getLength() {
    return 1;
  }

  public int getDelta() {
    return delta;
  }

  public Memory getMemory() {
    return memory;
  }

  public Register getPc() {
    return pc;
  }

  public void accept(InstructionVisitor instructionVisitor) {
    if (!instructionVisitor.visitMemory8BitReference(this))
      ImmutableOpcodeReference.super.accept(instructionVisitor);
  }

  public Object clone() throws CloneNotSupportedException {
    return new CachedMemory8BitReference(-1, memory, pc, delta);
  }

}

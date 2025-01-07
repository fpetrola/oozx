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

public class Memory16BitReference<T extends WordNumber> implements OpcodeReference<T> {
  private final Memory<T> memory;
  public T fetchedAddress;
  private final ImmutableOpcodeReference<T> pc;
  private final int delta;


  public Memory<T> getMemory() {
    return memory;
  }

  public ImmutableOpcodeReference<T> getPc() {
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

  public T read() {
    return fetchAddress();
  }

  public void write(T value) {
    T address = fetchAddress();
    Memory.write16Bits(memory, value, address);
  }

  protected T fetchAddress() {
    memory.disableReadListener();
    T pcValue = pc.read().plus(delta);
    fetchedAddress = Memory.read16Bits(memory, pcValue);
    memory.enableReadListener();

    return fetchedAddress;
  }

  public String toString() {
    T read = fetchedAddress;
    return read == null ? "" : Helper.formatAddress(read.intValue());
  }

  public int getLength() {
    return 2;
  }

  public void accept(InstructionVisitor instructionVisitor) {
    if (!instructionVisitor.visitMemory16BitReference(this))
      OpcodeReference.super.accept(instructionVisitor);
  }

  public Object clone() throws CloneNotSupportedException {
    T lastFetchedAddress = fetchedAddress;
    return new CachedMemory16BitReference(lastFetchedAddress, memory, pc, delta);
  }

}

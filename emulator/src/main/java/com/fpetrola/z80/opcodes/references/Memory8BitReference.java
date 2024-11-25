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

public class Memory8BitReference<T extends WordNumber> implements ImmutableOpcodeReference<T> {

  private final Memory<T> memory;
  private int delta;
  protected T fetchedAddress;
  private Register<T> pc;

  public Memory8BitReference(Memory memory, Register pc, int delta) {
    this.memory = memory;
    this.pc = pc;
    this.delta = delta;
  }

  public T read() {
    memory.disableReadListener();
    T read = memory.read(fetchAddress().plus(delta), delta, 0);
    memory.enableReadListener();
    return read;
  }

  public void write(T value) {
    memory.write(fetchAddress(), value);
  }

  protected T fetchAddress() {
    return fetchedAddress = pc.read();
  }

  public String toString() {
    T read = fetchedAddress;
    //return read == null ? "" : "0x" + Helper.convertToHex(read.intValue()) + "";
    return read == null ? "" : read.intValue() + "";
  }

  public int getLength() {
    return 1;
  }

  public void accept(InstructionVisitor instructionVisitor) {
    if (!instructionVisitor.visitMemory8BitReference(this))
      ImmutableOpcodeReference.super.accept(instructionVisitor);
  }

  public Object clone() throws CloneNotSupportedException {
    return new MyMemory8BitReference(fetchedAddress, memory, pc, delta);
  }

  private class MyMemory8BitReference extends Memory8BitReference<T> {
    public MyMemory8BitReference(T lastFetchedAddress, Memory<T> memory, Register<T> pc, int delta) {
      super(memory, pc, delta);
      this.fetchedAddress = lastFetchedAddress;
    }

    @Override
    protected T fetchAddress() {
      return fetchedAddress;
    }
  }
}

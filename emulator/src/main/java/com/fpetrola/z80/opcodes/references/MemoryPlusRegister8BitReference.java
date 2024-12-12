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

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

public class MemoryPlusRegister8BitReference<T extends WordNumber> implements OpcodeReference<T> {

  public T address;
  public T value;

  public Memory<T> getMemory() {
    return memory;
  }

  private Memory<T> memory;

  public ImmutableOpcodeReference<T> getTarget() {
    return target;
  }

  public void setTarget(ImmutableOpcodeReference<T> target) {
    this.target = target;
  }

  private ImmutableOpcodeReference<T> target;

  public int getValueDelta() {
    return valueDelta;
  }

  protected int valueDelta;
  public T fetchedRelative;

  public Register<T> getPc() {
    return pc;
  }

  private Register<T> pc;

  public MemoryPlusRegister8BitReference() {
  }

  public MemoryPlusRegister8BitReference(ImmutableOpcodeReference target, Memory memory, Register pc, int valueDelta) {
    this.target = target;
    this.memory = memory;
    this.pc = pc;
    this.valueDelta = valueDelta;
  }

  public T read() {
    T read = target.read();
    byte i = fetchRelative();
    address = read.plus(i);
    value = memory.read(address, 0);
    return value;
  }

  public void write(T value) {
    byte i = fetchRelative();
    address = target.read().plus(i);
    this.value= value;
    memory.write(address, value);
  }

  public byte fetchRelative() {
    T dd = memory.read(pc.read().plus(valueDelta), 0);
    if (fetchedRelative != dd) {
      fetchedRelative = dd;
    }
    return (byte) fetchedRelative.intValue();
  }

  public String toString() {
    byte dd = (byte) (fetchedRelative!=null? fetchedRelative.intValue(): 0);
    String string2 = (dd > 0 ? "+" : "-") + Helper.formatAddress(Math.abs(dd));
    String string = "IXY";// target.toString();
    return "(" + string + string2 + ")";
  }

  public int getLength() {
    return 1;
  }

  public Object clone() throws CloneNotSupportedException {
    T lastFetchedRelative = fetchedRelative;
    return new CachedMemoryPlusRegister8BitReference(lastFetchedRelative, (ImmutableOpcodeReference) target.clone(), memory, pc, valueDelta);
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitMemoryPlusRegister8BitReference(this);
  }
}

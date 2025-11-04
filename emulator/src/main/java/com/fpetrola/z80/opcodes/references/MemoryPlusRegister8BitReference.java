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

public class MemoryPlusRegister8BitReference implements OpcodeReference {
  public int address;
  public int value;

  public Memory getMemory() {
    return memory;
  }

  private Memory memory;

  public ImmutableOpcodeReference getTarget() {
    return target;
  }

  public void setTarget(ImmutableOpcodeReference target) {
    this.target = target;
  }

  private ImmutableOpcodeReference target;

  public int getValueDelta() {
    return valueDelta;
  }

  protected int valueDelta;
  public int fetchedRelative= -1;

  public Register getPc() {
    return pc;
  }

  private Register pc;

  public MemoryPlusRegister8BitReference() {
  }

  public MemoryPlusRegister8BitReference(ImmutableOpcodeReference target, Memory memory, Register pc, int valueDelta) {
    this.target = target;
    this.memory = memory;
    this.pc = pc;
    this.valueDelta = valueDelta;
  }

  public int read() {
    int read = target.read();
    byte i = fetchRelative();
    address = (read + (int) i) & 0xFFFF;
    value = memory.read(address, 0);
    return value;
  }

  public void write(int value) {
    byte i = fetchRelative();
    Integer wordNumber = target.read();
    address = (wordNumber + (int) i) & 0xFFFF;
    this.value = value;
    memory.write(address, value);
  }

  public byte fetchRelative() {
    Integer wordNumber = pc.read();
    Integer i = (wordNumber + valueDelta) & 0xFFFF;
    int dd = memory.read(i, 0);
    if (fetchedRelative != dd) {
      fetchedRelative = dd;
    }
    return (byte) (int) fetchedRelative;
  }

  public String toString() {
    byte dd = (byte) (fetchedRelative != -1 ? fetchedRelative : 0);
    String string2 = (dd > 0 ? "+" : "-") + Helper.formatAddress(Math.abs(dd));
    String string = "IXY";// target.toString();
    return "(" + string + string2 + ")";
  }

  public int getLength() {
    return 1;
  }

  public Object clone() throws CloneNotSupportedException {
    int lastFetchedRelative = fetchedRelative;
    return new CachedMemoryPlusRegister8BitReference(lastFetchedRelative, (ImmutableOpcodeReference) target.clone(), memory, pc, valueDelta);
  }

  public void accept(InstructionVisitor instructionVisitor) {
    instructionVisitor.visitMemoryPlusRegister8BitReference(this);
  }
}

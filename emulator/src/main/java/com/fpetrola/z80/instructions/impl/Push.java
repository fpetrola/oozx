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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Push<T extends WordNumber> extends AbstractInstruction<T> {
  public OpcodeReference<T> getTarget() {
    return target;
  }

  public void setTarget(OpcodeReference<T> target) {
    this.target = target;
  }

  protected OpcodeReference<T> target;
  protected final Register<T> sp;
  protected final Memory<T> memory;

  public Push(OpcodeReference target, Register<T> sp, Memory<T> memory) {
    this.target = target;
    this.sp = sp;
    this.memory = memory;
  }

  public int execute() {
    doPush(target.read(), sp, memory);
    return 5 + cyclesCost;
  }

  public static <T extends WordNumber> void doPush(T value, Register<T> sp, Memory<T> memory) {
//    System.out.println("push: " + value);
    memory.disableWriteListener();
    sp.decrement();
    sp.decrement();
    T address = sp.read();
    Memory.write16Bits(memory, value, address);
    memory.enableWriteListener();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    visitor.visitPush(this);
  }
}

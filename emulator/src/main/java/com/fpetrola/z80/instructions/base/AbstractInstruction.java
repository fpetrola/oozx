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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;

public abstract class AbstractInstruction<T extends WordNumber> implements Instruction<T> {
  protected int length = 1;
  protected int cyclesCost = 4;
  private T nextPC = null;

  protected AbstractInstruction() {
    cyclesCost += 1;
  }

  public String toString() {
    return getName();
  }

  protected String getName() {
    return getClass().getSimpleName();
  }

  public int getLength() {
    return length;
  }

  public void incrementLengthBy(int by) {
    length += by;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public State getState() {
    return null;
  }

  public void setNextPC(T address) {
    this.nextPC = address;
  }

  public T getNextPC() {
    return nextPC;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    visitor.visitingInstruction(this);
  }
}

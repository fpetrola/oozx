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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.CachedTableAluOperation;

public abstract class AbstractInstruction implements Instruction {
  protected int length = 1;
  private int nextPC = -1;
  private int rdelta;
  protected final AluOperation aluOperation;



  protected AbstractInstruction() {
    this.aluOperation = null;
  }

  protected AbstractInstruction(AluOperation aluOperation) {
    this.aluOperation = new CachedTableAluOperation(aluOperation);
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

  public void setNextPC(int address) {
    this.nextPC = address;
  }

  public int getNextPC() {
    return nextPC;
  }

  public void setRDelta(int rdelta) {
    this.rdelta = rdelta;
  }

  public int getRDelta() {
    return rdelta;
  }

  public AluOperation getAluOperation() {
    return aluOperation;
  }

  @Override
  public void accept(InstructionVisitor<?> visitor) {
    visitor.visitingInstruction(this);
  }
}

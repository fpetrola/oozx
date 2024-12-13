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

package com.fpetrola.z80.se.actions;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.se.RoutineExecution;

public class AddressAction {
  protected Instruction instruction;
  private RoutineExecution routineExecution;
  protected boolean alwaysTrue;
  protected boolean state;
  public int address;
  protected boolean pending;

  public AddressAction(int pcValue, RoutineExecution routineExecution) {
    this.address = pcValue;
    this.routineExecution = routineExecution;
  }

  public AddressAction(int pcValue, boolean b, RoutineExecution routineExecution, Instruction instruction, boolean alwaysTrue) {
    this(pcValue, b);

    this.routineExecution = routineExecution;
    this.instruction = instruction;
    this.alwaysTrue = alwaysTrue;
  }

  public AddressAction(int address, boolean pending) {
    this(address, null);
    this.pending = pending;
  }

  public boolean processBranch(Instruction instruction) {
    if (pending) {
      pending = false;
    }
    return true;
  }

  protected boolean getDoBranch() {
    boolean result = state;
    state = !state;
    result = alwaysTrue || result;
    return result;
  }

  public boolean isPending() {
    return pending;
  }
  public void setPending(boolean pending) {
    this.pending = pending;
  }

  @Override
  public String toString() {
    return "AddressAction{address=%d, instruction=%s, pending=%s}".formatted(address, instruction, pending);
  }

  protected int getNextPC(int address1) {
    if (pending) {
      pending= false;
      return address1;
    } else {
      return routineExecution.getNextPending().address;
    }
  }

  public int getNextPC() {
    return address;
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    if (alwaysTrue) {
      return getNextPC(currentPc);
    } else {
      return currentPc;
    }
  }
}







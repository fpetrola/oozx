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

import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class AddressAction<T extends WordNumber> {
  protected final RoutineExecutorHandler<T> routineExecutionHandler;
  protected Instruction instruction;
  protected boolean alwaysTrue;
  protected boolean branch;
  public int address;
  protected boolean pending;
  private int count;
  private ExecutionStackStorage executionStackStorage;

  public AddressAction(int pcValue, RoutineExecutorHandler routineExecutorHandler) {
    this.address = pcValue;
    this.routineExecutionHandler = routineExecutorHandler;
    executionStackStorage = routineExecutionHandler.getExecutionStackStorage().create();
  }

  public AddressAction(int pcValue, boolean b, Instruction instruction, boolean alwaysTrue, RoutineExecutorHandler routineExecutorHandler) {
    this(pcValue, b, routineExecutorHandler);
    this.instruction = instruction;
    this.alwaysTrue = alwaysTrue;
  }

  public AddressAction(int address, boolean pending, RoutineExecutorHandler routineExecutorHandler) {
    this(address, routineExecutorHandler);
    this.pending = pending;
  }

  public boolean processBranch(Instruction instruction) {
    if (pending) {
      pending = false;
    }
    return true;
  }

  protected boolean getDoBranch() {
    incCount();
    boolean result = branch;
    branch = !branch;
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
      pending = false;
      return address1;
    } else {
      return routineExecutionHandler.getCurrentRoutineExecution().getNextPending().address;
    }
  }

  protected void incCount() {
    if (!branch)
      executionStackStorage.save();
    else {
      if (!(instruction instanceof Ret<?>)) {
//        executionStackStorage.restore();
      }
    }

//    if (routineExecutionHandler.getPc().read().intValue() == 0x8d67)
//      System.out.println("dasfsssss!!!");
//    count++;
//    if (count > 2)
//      if (!(instruction instanceof Ret<?>))
//        System.out.println("adgadgdag");
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







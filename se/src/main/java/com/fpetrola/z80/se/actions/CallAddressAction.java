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

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class CallAddressAction<T extends WordNumber> extends AddressAction<T> {
  private final Call<T> call;
  private int calleeAddress;
  private RoutineExecution<T> calleeRoutineExecution;
  private boolean calleePending= true;

  public CallAddressAction(int pcValue, Call<T> call, boolean alwaysTrue, RoutineExecutorHandler<T> routineExecutorHandler) {
    super(pcValue, true, call, alwaysTrue, routineExecutorHandler);
    this.call = call;
    this.alwaysTrue = alwaysTrue;
  }

  public boolean processBranch(Instruction instruction) {
    boolean doBranch = getDoBranch();
    if (doBranch) {
      calleeAddress = call.getJumpAddress().intValue();
      calleeRoutineExecution = routineExecutionHandler.findRoutineExecutionAt(calleeAddress);
      if (calleeRoutineExecution != null) {
        calleePending = calleeRoutineExecution.isPending();
        if (calleePending)
          routineExecutionHandler.pushRoutineExecution(calleeRoutineExecution);
        return calleePending;
      } else {
        calleeRoutineExecution = routineExecutionHandler.createRoutineExecution(calleeAddress);
      }
    }
    return doBranch;
  }

  public int getNextPC() {
    return getNextPC(address);
  }

  @Override
  public boolean isPending() {
    RoutineExecution<T> currentRoutineExecution = routineExecutionHandler.getCurrentRoutineExecution();
    if (currentRoutineExecution == null)
      return false;
    else
      return pending || calleeRoutineExecution.isPending();
  }

  @Override
  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = branch;
    return super.getNext(executedInstructionAddress, currentPc);
  }
}

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

package com.fpetrola.z80.se.actions;

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class CallAddressAction extends AddressAction {
  private final Call call;
  private int calleeAddress;
  private RoutineExecution calleeRoutineExecution;
  private boolean calleePending= true;
  private boolean steppedOver;
  private RegisterName throughRegister;

  public CallAddressAction(int pcValue, Call call, boolean alwaysTrue, RoutineExecutorHandler routineExecutorHandler) {
    super(pcValue, true, call, alwaysTrue, routineExecutorHandler);
    this.call = call;
    this.alwaysTrue = alwaysTrue;
  }

  public boolean processBranch(Instruction instruction) {
    int target = call.getJumpAddress();
    if (!routineExecutionHandler.getRoutineManager().isCode(target)) {
      throughRegister = routineExecutionHandler.getStackAnalyzer().trampolineRegister(target);
      target = throughRegister == null ? -1 : routineExecutionHandler.getState().getRegister(throughRegister).read();
      if (!routineExecutionHandler.getRoutineManager().isCode(target)) {
        steppedOver = true;
        return false;
      }
    }
    boolean doBranch = getDoBranch();
    if (doBranch) {
      calleeAddress = target;
      calleeRoutineExecution = routineExecutionHandler.findRoutineExecutionAt(calleeAddress);
      if (calleeRoutineExecution != null) {
        calleePending = !routineExecutionHandler.getStackFrames().contains(calleeAddress) && calleeRoutineExecution.isPending();
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
    RoutineExecution currentRoutineExecution = routineExecutionHandler.getCurrentRoutineExecution();
    if (currentRoutineExecution == null)
      return false;
    else
      return pending || !steppedOver && calleeRoutineExecution != null && !routineExecutionHandler.getStackFrames().contains(calleeRoutineExecution.getStart()) && calleeRoutineExecution.isPending();
  }

  @Override
  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = branch && !steppedOver;
    if (steppedOver)
      return currentPc;
    if (throughRegister != null && currentPc == call.getJumpAddress())
      currentPc = routineExecutionHandler.getState().getRegister(throughRegister).read();
    if (currentPc == routineExecutionHandler.getRoutineManager().addressAfter(address))
      currentPc = currentPc + routineExecutionHandler.getStackAnalyzer().returnShifts.getOrDefault(call.getJumpAddress(), 0) & 0xffff;
    return super.getNext(executedInstructionAddress, currentPc);
  }
}

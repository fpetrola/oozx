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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class RetAddressAction extends AddressAction {
  private final int pcValue;
  private RoutineExecution<WordNumber> currentRoutineExecution;

  public RetAddressAction(Instruction<Boolean> instruction, int pcValue, boolean alwaysTrue, RoutineExecutorHandler routineExecutorHandler) {
    super(pcValue, true, instruction, alwaysTrue, routineExecutorHandler);
    this.pcValue = pcValue;
    this.alwaysTrue = alwaysTrue;
  }

  public boolean processBranch(Instruction instruction) {
    boolean doBranch = getDoBranch();
    super.processBranch(instruction);

    getCurrentRoutineExecution().setRetInstruction(pcValue);
    currentRoutineExecution = getCurrentRoutineExecution();
    if (!getCurrentRoutineExecution().hasPendingPoints() && doBranch) {
      getCurrentRoutineExecution().getRoutineExecutorHandler().popRoutineExecution();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int getNext(int executedInstructionAddress, int currentPc) {
    int result;
    int retInstruction = currentRoutineExecution.getRetInstruction();
    if (alwaysTrue) {
      pending = false;
      int result1 = currentPc;
      if (retInstruction == executedInstructionAddress && currentRoutineExecution.hasPendingPoints())
        result1 = currentRoutineExecution.getNextPending().address;
      result = result1;
    } else if (retInstruction == -1 || retInstruction == address) {
      result = currentPc;
    } else {
      AddressAction nextPending = currentRoutineExecution.getNextPending();
      result = nextPending == this ? currentPc : nextPending.address;
    }
    return result;
  }

}

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

package com.fpetrola.z80.se;

import com.fpetrola.z80.instructions.types.Instruction;

import java.util.List;

class RetAddressAction extends AddressAction {
  private final RoutineExecution routineExecution;
  private final int pcValue;

  public RetAddressAction(Instruction<Boolean> instruction, RoutineExecution routineExecution, int pcValue, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    super(pcValue, true, routineExecution, symbolicExecutionAdapter1, instruction, alwaysTrue);
    this.routineExecution = routineExecution;
    this.pcValue = pcValue;
    this.alwaysTrue = alwaysTrue;
  }

  public boolean processBranch(boolean doBranch, Instruction instruction) {
    doBranch= getDoBranch();
    super.processBranch(doBranch, instruction);

    routineExecution.retInstruction = pcValue;
    if (!routineExecution.hasPendingPoints() && doBranch) {
      symbolicExecutionAdapter.popFrame();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int getNext(int next, int pcValue) {
    if (alwaysTrue) {
      return genericGetNext(next, pcValue);
    } else {
      if (routineExecution.retInstruction == -1 || routineExecution.retInstruction == address)
        return super.getNext(next, pcValue);

      List<AddressAction> list = routineExecution.actions.stream().filter(addressAction -> addressAction.isPending() && addressAction != this).toList();
      if (list.isEmpty()) {
        return pcValue;
      } else {
        return list.get(0).address;
      }
    }
  }

  void setPendingAfterStep(SymbolicExecutionAdapter symbolicExecutionAdapter) {
    updatePending();
  }
}

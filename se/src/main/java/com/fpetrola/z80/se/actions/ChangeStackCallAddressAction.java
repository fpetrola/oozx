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

import java.util.List;

public class ChangeStackCallAddressAction<T extends WordNumber> extends BasicAddressAction<T> {
  private final RoutineExecutorHandler<T> routineExecutorHandler;
  private final List<RoutineExecution<T>> lastRoutineExecutions;

  public ChangeStackCallAddressAction(RoutineExecutorHandler<T> routineExecutorHandler, List<RoutineExecution<T>> lastRoutineExecutions, int pc1) {
    super(pc1, routineExecutorHandler);
    this.routineExecutorHandler = routineExecutorHandler;
    this.lastRoutineExecutions = lastRoutineExecutions;
  }

  public List<RoutineExecution<T>> getLastRoutineExecutions() {
    return lastRoutineExecutions;
  }

  public boolean processBranch(Instruction instruction) {
    if (hasPendingInStack()) {
      int jumpAddress = ((Call) instruction).getJumpAddress().intValue();
      routineExecutorHandler.createRoutineExecution(jumpAddress);
      return true;
    } else {
      super.processBranch(instruction);
      return false;
    }
  }

  private boolean hasPendingInStack() {
    return lastRoutineExecutions.stream().anyMatch(r -> r.hasPendingPoints());
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = branch;
    if (hasPendingInStack())
      return currentPc;
    else
      return routineExecutionHandler.getCurrentRoutineExecution().getNextPending().address;
  }

  public boolean isPending() {
    return pending || hasPendingInStack();
  }

}

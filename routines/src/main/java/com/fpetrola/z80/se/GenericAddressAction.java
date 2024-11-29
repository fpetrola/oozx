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

import java.util.Optional;

class GenericAddressAction extends AddressAction {

  private final RoutineExecution routineExecution;
  private final int pcValue;

  public GenericAddressAction(RoutineExecution routineExecution, int pcValue) {
    super(pcValue);
    this.routineExecution = routineExecution;
    this.pcValue = pcValue;
  }

  public int getNext(int next, int pcValue) {
    int result = pcValue;
    if (pending) {
      pending = false;
    }
    if (routineExecution.retInstruction == next && routineExecution.hasPendingPoints())
      result = routineExecution.getNextPending().address;
    return result;
  }

  @Override
  public int getNextPC() {
    int result = address;
    if (!pending) {
      Optional<AddressAction> addressAction1 = routineExecution.actions.stream().filter(a -> a.isPending()).findFirst();
      if (addressAction1.isPresent())
        result = addressAction1.get().address;
    }
    return result;
  }

  public boolean processBranch(boolean doBranch, Instruction instruction, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    AddressAction innerAddressAction = routineExecution.createConditionalAction(instruction, pcValue);
    if (!alwaysTrue)
      routineExecution.replaceAddressAction(innerAddressAction);

    boolean b = innerAddressAction.processBranch(doBranch, instruction, alwaysTrue, symbolicExecutionAdapter);
    innerAddressAction.setPending(true);
    return b;
  }
}

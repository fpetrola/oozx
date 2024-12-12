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
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

import java.util.Optional;

public class GenericAddressAction extends AddressAction {
  private final RoutineExecution routineExecution;

  public GenericAddressAction(RoutineExecution routineExecution, int pcValue, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    super(pcValue, routineExecution, symbolicExecutionAdapter1);
    this.routineExecution = routineExecution;
  }

  public int getNext(int next, int pcValue) {
    return genericGetNext(next, pcValue);
  }

  @Override
  public int getNextPC() {
    int result = address;
    if (!isPending())
      result = routineExecution.getNextPending().address;
    return result;
  }

  public boolean processBranch(Instruction instruction) {
    return false;
  }
}

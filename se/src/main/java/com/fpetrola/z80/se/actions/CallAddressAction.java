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
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

public class CallAddressAction extends AddressAction {
  private final Call call;

  public CallAddressAction(int pcValue, Call call, RoutineExecution routineExecution, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    super(pcValue, true, routineExecution, symbolicExecutionAdapter1, call, alwaysTrue);
    this.call = call;
    this.alwaysTrue = alwaysTrue;
  }

  public boolean processBranch(Instruction instruction) {
    boolean doBranch = getDoBranch();
    super.processBranch(instruction);

    if (doBranch) {
      int jumpAddress = call.getJumpAddress().intValue();
      symbolicExecutionAdapter.createRoutineExecution(jumpAddress);
    }
    return doBranch;
  }

  public void setReadyAfterStep() {
    updatePending();
  }

  public int getNext(int next, int pcValue) {
    if (alwaysTrue) {
      return genericGetNext(next, pcValue);
    } else {
      return super.getNext(next, pcValue);
    }
  }
}

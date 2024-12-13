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
import com.fpetrola.z80.se.DynamicJPData;
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.SEInstructionFactory;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

import java.util.LinkedList;

public class JPRegisterAddressAction extends AddressAction {
  private final RoutineExecution routineExecution;
  public DynamicJPData dynamicJPData;
  private LinkedList<Integer> cases = new LinkedList<>();

  public JPRegisterAddressAction(Instruction<Boolean> instruction, RoutineExecution routineExecution, int pcValue, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    super(pcValue, true, routineExecution, symbolicExecutionAdapter1, instruction, alwaysTrue);
    this.routineExecution = routineExecution;
  }

  public boolean processBranch(Instruction instruction) {
    pollCases();
    boolean doBranch = getDoBranch();

    super.processBranch(instruction);

    return doBranch;
  }

  @Override
  public int getNext(int next, int pcValue) {
    if (alwaysTrue) {
      if (!isPending()) {
        int result = pcValue;
        if (routineExecution.retInstruction == next && routineExecution.hasPendingPoints())
          result = routineExecution.getNextPending().address;
        return result;
      } else
        return genericGetNext(next, pcValue);
    } else {
      return super.getNext(next, pcValue);
    }
  }

  @Override
  public boolean isPending() {
    return super.isPending() || !cases.isEmpty();
  }

  public void setDynamicJPData(DynamicJPData dynamicJPData) {
    this.dynamicJPData = dynamicJPData;
    this.cases.addAll(dynamicJPData.cases);
  }

  public void pollCases() {
    Integer poll = cases.poll();
    if (poll != null) {
      SEInstructionFactory.SeJP jp = (SEInstructionFactory.SeJP) instruction;
      jp.lastData = WordNumber.createValue(poll);
    }
  }

}

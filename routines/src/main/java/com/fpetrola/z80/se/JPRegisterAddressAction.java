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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class JPRegisterAddressAction extends AddressAction {
  private final RoutineExecution routineExecution;
  private final State state;
  public DynamicJPData dynamicJPData;
  private LinkedList<Integer> cases = new LinkedList<>();

  public JPRegisterAddressAction(Instruction<Boolean> instruction, RoutineExecution routineExecution, int pcValue, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter1, State state) {
    super(pcValue, true, routineExecution, symbolicExecutionAdapter1, instruction, alwaysTrue);
    this.routineExecution = routineExecution;
    this.state = state;
  }

  public boolean processBranch(Instruction instruction) {
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
      if (true)
        return super.getNext(next, pcValue);
      List<AddressAction> list = routineExecution.actions.stream().filter(addressAction -> addressAction.isPending() && addressAction != this).toList();
      if (list.isEmpty()) {
        return pcValue;
      } else {
        return list.get(0).address;
      }
    }
  }

  @Override
  public boolean isPending() {
    return super.isPending() || !cases.isEmpty();
  }

  void setPendingAfterStep(SymbolicExecutionAdapter symbolicExecutionAdapter) {
    pending = false;
  }

  public void setDynamicJPData(DynamicJPData dynamicJPData) {
    this.dynamicJPData = dynamicJPData;
    this.cases.addAll(dynamicJPData.cases);
  }

  public void beforeStep() {
    Integer poll = cases.poll();
    if (poll != null) {
      JP jp = (JP) instruction;
      VirtualComposed16BitRegister positionOpcodeReference = (VirtualComposed16BitRegister) jp.getPositionOpcodeReference();
      positionOpcodeReference.lastData = WordNumber.createValue(poll);
    }
  }
}

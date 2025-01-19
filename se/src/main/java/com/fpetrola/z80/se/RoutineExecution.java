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


import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.actions.*;

import java.util.*;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class RoutineExecution<T extends WordNumber> {
  private final RoutineExecutorHandler<T> routineExecutorHandler;
  private int retInstruction = -1;
  private int start;
  private Map<Integer, AddressAction> actions = new HashMap<>();

  public RoutineExecution(RoutineExecutorHandler<T> routineExecutorHandler, int start) {
    this.routineExecutorHandler = routineExecutorHandler;
    this.start = start;
  }

  public boolean hasPendingPoints() {
    return actions.values().stream().anyMatch(AddressAction::isPending);
  }

  public AddressAction getNextPending() {
    return actions.values().stream().filter(AddressAction::isPending).findFirst().orElse(getActionOrCreateInAddress(retInstruction));
  }

  public List<AddressAction> getAllPending() {
    return actions.values().stream().filter(AddressAction::isPending).toList();
  }

  public boolean hasActionAt(int address) {
    return getAddressAction(address) != null;
  }

  public AddressAction getActionOrCreateInAddress(int pcValue) {
    AddressAction addressAction = getAddressAction(pcValue);
    if (addressAction == null) {
      addressAction = createAndAddGenericAction(pcValue);
    }

    return addressAction;
  }

  public AddressAction createAndAddGenericAction(int pcValue) {
    AddressAction addressAction = new GenericAddressAction(pcValue, routineExecutorHandler);
    replaceAddressAction(addressAction);
    return addressAction;
  }

  public AddressAction getAddressAction(int pcValue) {
    return actions.get(pcValue);
  }

  public void replaceAddressAction(AddressAction addressAction) {
    actions.put(addressAction.address, addressAction);
  }


  AddressAction replaceIfAbsent(int address, AddressAction addressAction2) {
    AddressAction addressAction1;
    if (!hasActionAt(address)) {
      addressAction1 = addressAction2;
      replaceAddressAction(addressAction1);
    } else
      addressAction1 = getAddressAction(address);

    return addressAction1;
  }

  public <T extends WordNumber> AddressAction createAddressAction(Instruction<Boolean> instruction, boolean alwaysTrue, int pcValue) {
    if (instruction instanceof Ret) {
      return new RetAddressAction(instruction, pcValue, alwaysTrue, routineExecutorHandler);
    } else if (instruction instanceof Call call) {
      return new CallAddressAction(pcValue, call, alwaysTrue, routineExecutorHandler);
    } else if (instruction instanceof JP jp && jp.getPositionOpcodeReference() instanceof Register) {
      return new JPRegisterAddressAction(instruction, pcValue, alwaysTrue, routineExecutorHandler,routineExecutorHandler.getStackAnalyzer().getInvocationsSet(pcValue));
    } else {
      return new ConditionalInstructionAddressAction(instruction, pcValue, alwaysTrue, routineExecutorHandler);
    }
  }

  public Optional<AddressAction> findActionOfType(Class<?> type) {
    return actions.values().stream().filter(addressAction1 -> type.isAssignableFrom(addressAction1.getClass())).findFirst();
  }

  public boolean hasRetInstruction() {
    return retInstruction != -1;
  }

  public void setRetInstruction(int retInstruction1) {
    retInstruction = retInstruction1;
  }

  public String toString() {
    return "RoutineExecution{start=%s, retInstruction=%s, pending=%s}".formatted(formatAddress(start), formatAddress(retInstruction), getAllPending().toString());
  }

  public int getStart() {
    return start;
  }

  public int getRetInstruction() {
    return retInstruction;
  }

  public boolean contains(int address) {
    return actions.containsKey(address);
  }
}

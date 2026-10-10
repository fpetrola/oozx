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

package com.fpetrola.z80.se;


import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.actions.*;

import java.util.*;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class RoutineExecution {
  private final RoutineExecutorHandler routineExecutorHandler;
  private int retInstruction = -1;
  private int start;
  private Map<Integer, AddressAction> actions = new HashMap<>();
  private List<RoutineExecution> callees = new ArrayList<>();
  private final Set<RoutineExecution> callers = new HashSet<>();
  private boolean nothingPending, noPendingPoints;

  public RoutineExecution(RoutineExecutorHandler routineExecutorHandler, int start) {
    this.routineExecutorHandler = routineExecutorHandler;
    this.start = start;
  }

  public boolean hasPendingPoints() {
    if (!noPendingPoints)
      noPendingPoints = !hasPendingPoints(new java.util.HashSet<>());
    return !noPendingPoints;
  }

  public boolean hasPendingPoints(java.util.Set<RoutineExecution> visited) {
    return !noPendingPoints && visited.add(this) && pendingAction(visited);
  }

  public void dependsOn(RoutineExecution callee) {
    callee.callers.add(this);
    invalidate();
  }

  public void invalidate() {
    invalidate(new HashSet<>());
  }

  private void invalidate(Set<RoutineExecution> seen) {
    if (seen.add(this)) {
      nothingPending = noPendingPoints = false;
      callers.forEach(caller -> caller.invalidate(seen));
    }
  }

  private boolean pendingAction(java.util.Set<RoutineExecution> visited) {
    return actions.values().stream().anyMatch(action -> action.isPending(visited));
  }

  public AddressAction getNextPending() {
    AddressAction next = actions.values().stream().filter(AddressAction::isPending).findFirst().orElseGet(this::retInstructionAction);
    next.resume();
    return next;
  }

  private AddressAction retInstructionAction() {
    return getAddressAction(retInstruction) instanceof BasicAddressAction ? getActionOrCreateInAddress(-1) : getActionOrCreateInAddress(retInstruction);
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
    AddressAction replaced = actions.put(addressAction.address, addressAction);
    if (replaced != null)
      addressAction.keepStackStorageOf(replaced);
    addressAction.ownedBy(this);
    invalidate();
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

  public  AddressAction createAddressAction(Instruction instruction, boolean alwaysTrue, int pcValue) {
    if (instruction instanceof Ret) {
      return new RetAddressAction(instruction, pcValue, alwaysTrue, routineExecutorHandler);
    } else if (instruction instanceof Call call) {
      return new CallAddressAction(pcValue, call, alwaysTrue, routineExecutorHandler);
    } else if (instruction instanceof JP jp && jp.getPositionOpcodeReference() instanceof Register) {
      return new JPRegisterAddressAction(instruction, pcValue, alwaysTrue, routineExecutorHandler, routineExecutorHandler.getStackAnalyzer().getInvocationsSet(pcValue));
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

  public void addCallee(RoutineExecution routineExecution) {
    callees.add(routineExecution);
    dependsOn(routineExecution);
  }

  public boolean isPending() {
    if (!nothingPending)
      nothingPending = !isPending(new java.util.HashSet<>());
    return !nothingPending;
  }

  public boolean isPending(java.util.Set<RoutineExecution> visited) {
    return !nothingPending && visited.add(this) && (pendingAction(visited) || callees.stream().anyMatch(callee -> !routineExecutorHandler.getStackFrames().contains(callee.start) && callee.isPending(visited)));
  }
}

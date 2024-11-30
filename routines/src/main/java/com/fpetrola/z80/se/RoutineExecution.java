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


import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class RoutineExecution {
  private final int minimalValidCodeAddress;
  public int retInstruction = -1;
  public int start;
  public LinkedList<AddressAction> actions = new LinkedList<>();
  private SymbolicExecutionAdapter symbolicExecutionAdapter;

  public RoutineExecution(int minimalValidCodeAddress, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    this.minimalValidCodeAddress = minimalValidCodeAddress;
    symbolicExecutionAdapter = symbolicExecutionAdapter1;
  }

  public boolean hasPendingPoints() {
    return actions.stream().anyMatch(AddressAction::isPending);
  }

  public AddressAction getNextPending() {
    return actions.stream().filter(AddressAction::isPending).findFirst().orElse(getActionInAddress(retInstruction));
  }

  public boolean hasActionAt(int address) {
    return getAddressAction(address) != null;
  }

  public AddressAction getActionInAddress(int pcValue) {
    AddressAction addressAction = getAddressAction(pcValue);
    if (addressAction == null) {
      addressAction = new GenericAddressAction(this, pcValue, symbolicExecutionAdapter);
      replaceAddressAction(addressAction);
    }

    return addressAction;
  }

  public AddressAction getAddressAction(int pcValue) {
    List<AddressAction> list = actions.stream().filter(a -> a.address == pcValue).toList();
    if (list.isEmpty())
      return null;
    else
      return list.get(0);
  }

  public void replaceAddressAction(AddressAction addressAction) {
    Optional<AddressAction> first = actions.stream().filter(a -> a.address == addressAction.address).findFirst();
    if (first.isPresent())
      actions.set(actions.indexOf(first.get()), addressAction);
    else {
      actions.offer(addressAction);
    }

    if (actions.stream().filter(a -> a.address == addressAction.address).toList().size() > 1)
      System.out.println("sdgsdgdsg11111");
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

  public <T extends WordNumber> AddressAction createAddressAction(Instruction<Boolean> instruction, boolean alwaysTrue, int pcValue, SymbolicExecutionAdapter<T> symbolicExecutionAdapter) {
    if (instruction instanceof Ret) {
      return new RetAddressAction(instruction, this, pcValue, alwaysTrue, symbolicExecutionAdapter);
    } else if (instruction instanceof Call call) {
      return new CallAddressAction(pcValue, call, this, alwaysTrue, symbolicExecutionAdapter);
    } else {
      return new ConditionalInstructionAddressAction(instruction, this, pcValue, alwaysTrue, symbolicExecutionAdapter);
    }
  }
}

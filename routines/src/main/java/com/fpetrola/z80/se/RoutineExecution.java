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
import com.fpetrola.z80.instructions.types.Instruction;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class RoutineExecution {
  private final int minimalValidCodeAddress;
  public int retInstruction = -1;
  public int start;
  public LinkedList<AddressAction> actions = new LinkedList<>();

  public RoutineExecution(int minimalValidCodeAddress) {
    this.minimalValidCodeAddress = minimalValidCodeAddress;
  }

  public boolean hasPendingPoints() {
    return actions.stream().anyMatch(AddressAction::isPending);
  }

  private AddressAction peekNextPending() {
    return actions.peek();
  }

  public AddressAction createConditionalAction(Instruction instruction, int pcValue) {
    AddressAction conditionalAddressAction;
    if (instruction instanceof Ret ret) {
      conditionalAddressAction = new RetAddressAction(this, pcValue);
    } else if (instruction instanceof Call call) {
      conditionalAddressAction = new CallAddressAction(pcValue, call);
    } else {
      conditionalAddressAction = new ConditionalInstructionAddressAction(this, pcValue);
    }
    return conditionalAddressAction;
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
      addressAction = createActionForConditionals(pcValue);
      replaceAddressAction(addressAction);
    }

    return addressAction;
  }

  private AddressAction createActionForConditionals(int pcValue) {
    AddressAction addressAction;
    addressAction = new GenericAddressAction(this, pcValue);

    return addressAction;
  }

  private AddressAction getAddressAction(int pcValue) {
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


  int getNext(int pcValue) {
    int next = pcValue;
    if (!hasActionAt(pcValue) && pcValue >= minimalValidCodeAddress) {
      AddressAction addressAction = getActionInAddress(pcValue);
      replaceAddressAction(addressAction);
    } else {
      if (!hasPendingPoints()) {
        if (retInstruction == -1)
          System.out.print("");

        next = retInstruction;
      } else
        next = getNextPending().address;
    }
    return next;
  }

}

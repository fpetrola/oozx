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
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class GenericAddressAction extends AddressAction {
  public GenericAddressAction(int pcValue, RoutineExecutorHandler routineExecutorHandler) {
    super(pcValue, routineExecutorHandler);
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = false;
    return currentPc;
  }

  public int getNextPC() {
    return getNextPC(address);
  }

  public boolean processBranch(Instruction instruction) {
    return false;
  }
}

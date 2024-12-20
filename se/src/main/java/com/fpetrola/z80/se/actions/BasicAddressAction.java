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

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.RoutineExecutorHandler;

public class BasicAddressAction<T extends WordNumber> extends AddressAction<T> {
  public BasicAddressAction(int address, RoutineExecutorHandler routineExecutorHandler) {
    super(address, true, routineExecutorHandler);
  }

  public BasicAddressAction(int address, RoutineExecutorHandler routineExecutorHandler, boolean pending) {
    super(address, pending, routineExecutorHandler);
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    setPending(false);
    return super.getNext(executedInstructionAddress, currentPc);
  }
}

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
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.actions.AddressAction;

import java.util.LinkedList;
import java.util.Set;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class JumpUsingRetAddressAction<T extends WordNumber> extends AddressAction<T> {
  private final Set<Integer> jumpAddresses;
  private final LinkedList<Integer> cases;
  private Integer currentCase;

  public JumpUsingRetAddressAction(int pcValue, Set<Integer> jumpAddresses, RoutineExecutorHandler<T> routineExecutorHandler) {
    super(pcValue, routineExecutorHandler);
    this.jumpAddresses = jumpAddresses;
    this.cases = new LinkedList<>(jumpAddresses);
  }

  public boolean processBranch(Instruction instruction) {
    boolean doBranch = getDoBranch();

    if (doBranch) {
      State<T> state = routineExecutionHandler.getState();
      pollNextCase();
      Memory.write16Bits(state.getMemory(), createValue(currentCase), state.getRegisterSP().read());
    }
    return doBranch;
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = false;
    if (currentCase != null)
      return currentCase;
    else if (!cases.isEmpty())
      return executedInstructionAddress;
    else
      return super.getNext(executedInstructionAddress, currentPc);
  }

  private void pollNextCase() {
    currentCase = cases.poll();
  }

  public boolean isPending() {
    return pending || !cases.isEmpty();
  }
}

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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.se.RoutineExecutorHandler;
import com.fpetrola.z80.se.StackListener;

import java.util.LinkedList;
import java.util.Set;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class JPRegisterAddressAction<T extends WordNumber> extends AddressAction<T> {
  private final Set<Integer> invocations;
  private LinkedList<Integer> cases = new LinkedList<>();
  private Integer currentCase;

  public JPRegisterAddressAction(Instruction<Boolean> instruction, int pcValue, boolean alwaysTrue, RoutineExecutorHandler routineExecutorHandler, Set<Integer> invocations) {
    super(pcValue, true, instruction, alwaysTrue, routineExecutorHandler);
    this.invocations = invocations;
    cases.addAll(invocations);
  }

  public boolean processBranch(Instruction instruction) {
    ConditionalInstruction conditionalInstruction = (ConditionalInstruction) instruction;
    boolean doBranch = getDoBranch();

    if (doBranch) {
      State<T> state = routineExecutionHandler.getState();
      pollNextCase();

      Register hlRegister = (Register) conditionalInstruction.getPositionOpcodeReference();
      hlRegister.write(createValue(currentCase));
      routineExecutionHandler.getStackAnalyzer().listenEvents(new StackListener() {
        public boolean simulatedCall(int pcValue, int jumpAddress, Set<Integer> jumpAddresses, int returnAddress) {
          routineExecutionHandler.createRoutineExecution(currentCase);
          System.out.println("JP (HL) -> " + Helper.formatAddress(jumpAddress));
          return StackListener.super.simulatedCall(pcValue, jumpAddress, jumpAddresses, returnAddress);
        }
      });
    }
    return doBranch;
  }

  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = false;
    if (!cases.isEmpty())
      return currentCase;
    else
      return super.getNext(executedInstructionAddress, currentPc);
  }

  private void pollNextCase() {
    currentCase = cases.poll();
  }

  @Override
  public boolean isPending() {
    return pending || !cases.isEmpty();
  }
}

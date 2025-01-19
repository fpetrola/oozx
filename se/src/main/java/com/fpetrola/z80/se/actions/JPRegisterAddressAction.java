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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.RoutineExecutorHandler;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.se.instructions.SEInstructionFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class JPRegisterAddressAction<T extends WordNumber> extends AddressAction<T> {
  private final Set invocations;
  //  public DynamicJPData dynamicJPData;
  private LinkedList<Integer> cases = new LinkedList<>();

  public JPRegisterAddressAction(Instruction<Boolean> instruction, int pcValue, boolean alwaysTrue, RoutineExecutorHandler routineExecutorHandler, Set invocations) {
    super(pcValue, true, instruction, alwaysTrue, routineExecutorHandler);
    this.invocations = invocations;
    cases.addAll(invocations);
  }

  public boolean processBranch(Instruction instruction) {
    ConditionalInstruction conditionalInstruction = (ConditionalInstruction) instruction;
//    pollCases();
    boolean doBranch = getDoBranch();

    if (doBranch) {
      State<T> state = routineExecutionHandler.getState();

      routineExecutionHandler.getStackAnalyzer().listenEvents(new StackListener() {
        public boolean simulatedCall(int pcValue, int i) {
          int jumpAddress = conditionalInstruction.calculateJumpAddress().intValue();
          if (jumpAddress > 16384)
            routineExecutionHandler.createRoutineExecution(jumpAddress);
          else System.out.println("JP (HL) -> " + Helper.formatAddress(jumpAddress));
          return StackListener.super.simulatedCall(pcValue, i);
        }
      });
    }
    return doBranch;
  }

  @Override
  public int getNext(int executedInstructionAddress, int currentPc) {
    pending = false;
    Integer poll = cases.poll();
    if (poll != null)
      return poll;
    else
      return currentPc;
  }

  @Override
  public boolean isPending() {
    return pending || !cases.isEmpty();
  }

//  public void setDynamicJPData(DynamicJPData dynamicJPData) {
//    this.dynamicJPData = dynamicJPData;
//    this.cases.addAll(dynamicJPData.cases);
//  }

//  public void pollCases() {
//    Integer poll = cases.poll();
//    if (poll != null) {
//      SEInstructionFactory.SeJP jp = (SEInstructionFactory.SeJP) instruction;
//      jp.lastData = WordNumber.createValue(poll);
//    }
//  }

//  public static class DynamicJPData {
//    private final int pc;
//    private final int pointer;
//    private final int pointerAddress;
//    public Set<Integer> cases = new HashSet<>();
//
//    public DynamicJPData(int pc, int pointer, int pointerAddress) {
//      this.pc = pc;
//      this.pointer = pointer;
//      this.pointerAddress = pointerAddress;
//    }
//
//    public void addCase(int aCase) {
//      System.out.println("0x" + Helper.formatAddress(pointerAddress()) + ":  " + Helper.formatAddress(aCase));
//      cases.add(aCase);
//    }
//
//    public int pc() {
//      return pc;
//    }
//
//    public int pointer() {
//      return pointer;
//    }
//
//    public int pointerAddress() {
//      return pointerAddress;
//    }
//  }
}

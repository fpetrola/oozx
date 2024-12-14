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

import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.*;

public class PopReturnAddress<T extends WordNumber> extends Pop<T> implements IPopReturnAddress<T> {
  private final SymbolicExecutionAdapter<T> symbolicExecutionAdapter;
  public final Register<T> pc;
  private int previousPc = -1;
  private ReturnAddressWordNumber returnAddress0;
  private int popAddress;

  @Override
  public ReturnAddressWordNumber getReturnAddress() {
    return returnAddress;
  }

  private ReturnAddressWordNumber returnAddress;

  public PopReturnAddress(SymbolicExecutionAdapter symbolicExecutionAdapter, OpcodeReference target, Register<T> sp, Memory<T> memory, Register<T> flag, Register<T> pc) {
    super(target, sp, memory, flag);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.pc = pc;
  }

  public int execute() {
    setNextPC(null);
    returnAddress = null;
    var read = Memory.read16Bits(memory, sp.read());

    if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
      var pcValue = pc.read().intValue();
      returnAddress0 = returnAddressWordNumber;
      previousPc = symbolicExecutionAdapter.lastPc;
      popAddress = pcValue;

      var routineExecutorHandler1 = symbolicExecutionAdapter.routineExecutorHandler;

      var lastRoutineExecution = routineExecutorHandler1.getCurrentRoutineExecution();
      var routineExecution = routineExecutorHandler1.getCallerRoutineExecution();

      routineExecution.replaceAddressAction(new AddressActionDelegate(pcValue + 1, routineExecutorHandler1));
      routineExecution.replaceAddressAction(new AddressActionDelegate(returnAddressWordNumber.intValue(), routineExecutorHandler1));

      var addressAction1 = new BasicAddressAction(popAddress, routineExecutorHandler1);
      addressAction1.setPending(false);
      lastRoutineExecution.replaceAddressAction(addressAction1);
      routineExecution.replaceAddressAction(new BasicAddressAction(returnAddressWordNumber.pc, routineExecutorHandler1) {
        public boolean processBranch(Instruction instruction) {
          if (lastRoutineExecution.hasPendingPoints()) {
            symbolicExecutionAdapter.createRoutineExecution(((Call) instruction).getJumpAddress().intValue());
            return true;
          } else {
            super.processBranch(instruction);
            return false;
          }
        }

        public int getNext(int executedInstructionAddress, int currentPc) {
          if (lastRoutineExecution.hasPendingPoints())
            return currentPc;
          else
            return getCurrentRoutineExecution().getNextPending().address;
        }
      });
      returnAddress = returnAddressWordNumber;
      T read1 = doPop(memory, sp);
      target.write(read1);
      symbolicExecutionAdapter.popRoutineExecution();
      if (!lastRoutineExecution.hasRetInstruction())
        lastRoutineExecution.setRetInstruction(pcValue);
    } else {
      symbolicExecutionAdapter.checkNextSP();
      T read1 = doPop(memory, sp);
      if (read1 == null) {
        System.out.print("");
      }
      target.write(read1);
    }

    return 0;
  }

  protected String getName() {
    return "Pop_";
  }

  @Override
  public int getPreviousPc() {
    return previousPc;
  }

  @Override
  public ReturnAddressWordNumber getReturnAddress0() {
    return returnAddress0;
  }

  @Override
  public int getPopAddress() {
    return popAddress;
  }
}

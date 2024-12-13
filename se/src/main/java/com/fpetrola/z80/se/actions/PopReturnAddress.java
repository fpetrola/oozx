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
import com.fpetrola.z80.se.IPopReturnAddress;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.se.RoutineExecution;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

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
    final T read = Memory.read16Bits(memory, sp.read());

    if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
      returnAddress0 = returnAddressWordNumber;

      // target.write(createValue(0));

      RoutineExecution lastRoutineExecution = symbolicExecutionAdapter.getRoutineExecution();
//      if (symbolicExecutionAdapter.lastPc != lastRoutineExecution.lastPc)
//        System.out.println("lastpc!!!");
      previousPc = symbolicExecutionAdapter.lastPc;
      RoutineExecution routineExecution = symbolicExecutionAdapter.routineExecutions.get(symbolicExecutionAdapter.stackFrames.get(symbolicExecutionAdapter.stackFrames.size() - 2));
      int address2 = pc.read().intValue() + 1;

      routineExecution.replaceAddressAction(new AddressActionDelegate(address2, symbolicExecutionAdapter, getState()));
      routineExecution.replaceAddressAction(new AddressActionDelegate(returnAddressWordNumber.intValue(), symbolicExecutionAdapter, getState()));

      popAddress = pc.read().intValue();
      BasicAddressAction addressAction1 = new BasicAddressAction(popAddress);
      addressAction1.setPending(false);
      lastRoutineExecution.replaceAddressAction(addressAction1);
      routineExecution.replaceAddressAction(new BasicAddressAction(returnAddressWordNumber.pc) {
        @Override
        public boolean processBranch(Instruction instruction) {
          if (lastRoutineExecution.hasPendingPoints()) {
            Call call = (Call) instruction;
            int jumpAddress = call.getJumpAddress().intValue();
            symbolicExecutionAdapter.createRoutineExecution(jumpAddress);
            return true;
          } else {
            super.processBranch(instruction);
            return false;
          }
        }

        @Override
        public int getNext(int executedInstructionAddress, int currentPc) {
          if (lastRoutineExecution.hasPendingPoints())
            return currentPc;
          else
            return routineExecution.getNextPending().address;
        }
      });
      {
        returnAddress = returnAddressWordNumber;
        T read1 = doPop(memory, sp);
        target.write(read1);
        symbolicExecutionAdapter.popFrame();
      }
      if (!lastRoutineExecution.hasRetInstruction())
        lastRoutineExecution.setRetInstruction(this.pc.read().intValue());
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

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
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class PopReturnAddress<T extends WordNumber> extends Pop<T> {
  private final SymbolicExecutionAdapter<T> symbolicExecutionAdapter;
  public final Register<T> pc;
  public int previousPc = -1;
  public ReturnAddressWordNumber returnAddress0;
  public int popAddress;

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

      previousPc = symbolicExecutionAdapter.lastPc;
      RoutineExecution routineExecution = symbolicExecutionAdapter.routineExecutions.get(symbolicExecutionAdapter.stackFrames.get(symbolicExecutionAdapter.stackFrames.size() - 2));
      int address2 = pc.read().intValue() + 1;

      routineExecution.replaceAddressAction(new BasicAddressAction(address2, symbolicExecutionAdapter));
      routineExecution.replaceAddressAction(new AddressActionDelegate(returnAddressWordNumber.intValue(), symbolicExecutionAdapter));

      RoutineExecution lastRoutineExecution = symbolicExecutionAdapter.getRoutineExecution();
      popAddress = pc.read().intValue();
      BasicAddressAction addressAction1 = new BasicAddressAction(popAddress, symbolicExecutionAdapter);
      addressAction1.setPending(false);
      lastRoutineExecution.replaceAddressAction(addressAction1);
      routineExecution.replaceAddressAction(new BasicAddressAction(returnAddressWordNumber.pc, symbolicExecutionAdapter) {
        @Override
        public boolean processBranch(boolean doBranch, Instruction instruction) {
          doBranch= getDoBranch();

          if (lastRoutineExecution.hasPendingPoints()) {
            Call call = (Call) instruction;
            int jumpAddress = call.getJumpAddress().intValue();
            symbolicExecutionAdapter.createRoutineExecution(jumpAddress);
            return true;
          } else {
            super.processBranch(false, instruction);
            return false;
          }
        }

        @Override
        public int getNext(int next, int pcValue) {
          if (lastRoutineExecution.hasPendingPoints())
            return pcValue;
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
      if (lastRoutineExecution.retInstruction == -1)
        lastRoutineExecution.retInstruction = pc.read().intValue();
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

  public class AddressActionDelegate extends BasicAddressAction {
    public AddressActionDelegate(int address2, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
      super(address2, symbolicExecutionAdapter1);
    }

    public boolean processBranch(boolean doBranch, Instruction instruction) {
      return false;
    }

    @Override
    public int getNext(int next, int pcValue) {
      return super.getNext(next, pcValue);
    }
  }
}

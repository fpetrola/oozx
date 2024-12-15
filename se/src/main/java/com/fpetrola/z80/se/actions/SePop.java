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

import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.*;

public class SePop<T extends WordNumber> extends Pop<T> implements IPopReturnAddress<T> {
  private final SymbolicExecutionAdapter<T> symbolicExecutionAdapter;
  private int previousPc = -1;
  private int popAddress;

  private ReturnAddressWordNumber returnAddress;

  public SePop(SymbolicExecutionAdapter<T> symbolicExecutionAdapter, OpcodeReference<T> target, Register<T> sp, Memory<T> memory, Register<T> flag) {
    super(target, sp, memory, flag);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
  }

  public int execute() {
    setNextPC(null);
    returnAddress = null;
    var read = Memory.read16Bits(memory, sp.read());

    if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
      RoutineExecutorHandler<T> routineExecutorHandler = symbolicExecutionAdapter.routineExecutorHandler;
      var pc = routineExecutorHandler.getPc();
      var pcValue = pc.read().intValue();

      previousPc = symbolicExecutionAdapter.lastPc;
      popAddress = pcValue;
      returnAddress = returnAddressWordNumber;

      var lastRoutineExecution = routineExecutorHandler.getCurrentRoutineExecution();
      var routineExecution = routineExecutorHandler.getCallerRoutineExecution();

      routineExecution.replaceAddressAction(new AddressActionDelegate<>(pcValue + 1, routineExecutorHandler));
      routineExecution.replaceAddressAction(new AddressActionDelegate<>(returnAddressWordNumber.intValue(), routineExecutorHandler));
      lastRoutineExecution.replaceAddressAction(new BasicAddressAction<T>(popAddress, routineExecutorHandler, false));
      routineExecution.replaceAddressAction(new PopReturnCallAddressAction<>(routineExecutorHandler, lastRoutineExecution, returnAddressWordNumber.pc));

      target.write(doPop(memory, sp));

      routineExecutorHandler.popRoutineExecution();
      if (!lastRoutineExecution.hasRetInstruction())
        lastRoutineExecution.setRetInstruction(pcValue);
    } else {
      symbolicExecutionAdapter.checkNextSP();
      target.write(doPop(memory, sp));
    }

    return 0;
  }

  protected String getName() {
    return "Pop_";
  }

  public int getPreviousPc() {
    return previousPc;
  }

  public int getPopAddress() {
    return popAddress;
  }

  public ReturnAddressWordNumber getReturnAddress() {
    return returnAddress;
  }
}

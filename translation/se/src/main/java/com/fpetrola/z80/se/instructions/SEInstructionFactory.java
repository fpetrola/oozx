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

package com.fpetrola.z80.se.instructions;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.impl.Halt;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

public class SEInstructionFactory extends DefaultInstructionFactory {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  private final DataflowService dataflowService;

  public void reset() {
  }

  public SEInstructionFactory(SymbolicExecutionAdapter symbolicExecutionAdapter, State state, DataflowService dataflowService1) {
    super(state);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    dataflowService = dataflowService1;
  }

  public Ld Ld(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Ld(target, source, flag) {
      public void execute() {
        boolean storesThroughRegister = target instanceof MemoryPlusRegister8BitReference
            || target instanceof IndirectMemory8BitReference indirect && indirect.getTarget() instanceof Register;
        if (storesThroughRegister)
          source.read();
        else
          super.execute();
      }

      protected String getName() {
        return "Ld_";
      }
    };
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new JP(target, condition, pc) {
      public void execute() {
        if (positionOpcodeReference instanceof Register) {
          if (condition.conditionMet(this)) {
            int address = calculateJumpAddress();
            setJumpAddress(address);
            setNextPC(address);
          } else
            setNextPC(-1);
        } else
          super.execute();
      }

      public int calculateJumpAddress() {
        int wordNumber = super.calculateJumpAddress();
        if (wordNumber == 40838)
          wordNumber= 34463;
        return wordNumber;
      }
    };
  }

  public Halt Halt() {
    return new Halt(state) {
      public void execute() {
      }
    };
  }
}

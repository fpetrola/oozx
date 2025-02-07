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
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.se.DirectAccessWordNumber;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.se.actions.PushReturnAddress;

public class SEInstructionFactory<T extends WordNumber> extends DefaultInstructionFactory<T> {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  private final DataflowService<T> dataflowService;

  public void reset() {
  }

  public SEInstructionFactory(SymbolicExecutionAdapter symbolicExecutionAdapter, State state, DataflowService<T> dataflowService1) {
    super(state);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    dataflowService = dataflowService1;
  }

  public Ld<T> Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return new Ld<T>(target, source, flag) {
      public int execute() {
//        if (target instanceof Register<T> register) {
//          if (register.getName().equals(RegisterName.SP.name())) {
//            symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().printStack();
//            return 0;
//          }
//        }

//        if (target instanceof Register<T> register) {
//          if (register.getName().equals(RegisterName.SP.name())) {
//            System.out.println("LD SP at: " + Helper.formatAddress(pc.read().intValue()));
//            if (pc.read().intValue() != 0x8185) {
//              int i = source.read().intValue();
//              if (source instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
//                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().restoringSP(i);
//              } else
//                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().changingSP(i);
//            }
//            return 0;
//          }
//        }

        if (target instanceof MemoryPlusRegister8BitReference<T>) {
          T value = source.read();
          return 0;
        }
        if (target instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference && indirectMemory8BitReference.getTarget() instanceof Register<T>) {
          T value = source.read();
          return cyclesCost;
        }
//        if (source instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
//          T value = source.read();
//          T address = indirectMemory16BitReference.address;
//          T aLU8Assign = value;
//          target.write((T) new DirectAccessWordNumber(aLU8Assign.intValue(), pc.read().intValue(), address.intValue()));
//          return 1;
//        } else if (source instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
//          T value = source.read();
//          T address = indirectMemory8BitReference.address;
//          T aLU8Assign = value;
//          target.write((T) new DirectAccessWordNumber(aLU8Assign.intValue(), pc.read().intValue(), address.intValue()));
//          return cyclesCost;
//        } else
          return super.execute();
      }

      protected String getName() {
        return "Ld_";
      }
    };
  }

  public Push Push(OpcodeReference target) {
    return new PushReturnAddress(symbolicExecutionAdapter, target, sp, memory);
  }
}

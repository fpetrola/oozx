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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.se.DirectAccessWordNumber;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.se.actions.JPRegisterAddressAction;
import com.fpetrola.z80.se.actions.PushReturnAddress;

import java.util.HashMap;
import java.util.Map;

public class SEInstructionFactory<T extends WordNumber> extends DefaultInstructionFactory<T> {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  public static Map<Integer, JPRegisterAddressAction.DynamicJPData> dynamicJP = new HashMap<>();
  private final DataflowService<T> dataflowService;

  public void reset() {
    dynamicJP.clear();
    SeJP.lastData= null;
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

        if (target instanceof Register<T> register) {
          if (register.getName().equals(RegisterName.SP.name())) {
            System.out.println("LD SP at: " + Helper.formatAddress(pc.read().intValue()));
            if (pc.read().intValue() != 0x8185) {
              int i = source.read().intValue();
              if (source instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().restoringSP(i);
              } else
                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().changingSP(i);
            }
            return 0;
          }
        }

        if (source instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
          T value = source.read();
          T address = indirectMemory16BitReference.address;
          T aLU8Assign = value;
          target.write((T) new DirectAccessWordNumber(aLU8Assign.intValue(), pc.read().intValue(), address.intValue()));
          return 1;
        } else if (source instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
          T value = source.read();
          T address = indirectMemory8BitReference.address;
          T aLU8Assign = value;
          target.write((T) new DirectAccessWordNumber(aLU8Assign.intValue(), pc.read().intValue(), address.intValue()));
          return cyclesCost;
        } else
          return super.execute();
      }

      protected String getName() {
        return "Ld_";
      }
    };
  }

  public Ret Ret(Condition condition) {
    return new Ret<T>(condition, sp, memory, pc) {
      public int execute() {
//            if (!getRoutineExecution().hasActionAt(getPcValue()))
//              getRoutineExecution().replaceAddressAction(new RetAddressAction(getRoutineExecution(), getPcValue()));
//            addressAction = getRoutineExecution().getActionInAddress(getPcValue());

        int execute = super.execute();
        return execute;
      }

      protected String getName() {
        return "Ret_";
      }
    };
  }

  public Push Push(OpcodeReference target) {
    return new PushReturnAddress(symbolicExecutionAdapter, target, sp, memory);
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new SeJP(target, condition);
  }

  public class SeJP extends JP<T> {

    public static WordNumber lastData;

    public SeJP(ImmutableOpcodeReference target, Condition condition) {
      super(target, condition, SEInstructionFactory.this.pc);
    }

//    @Override
//    public T calculateJumpAddress() {
//      T t = super.calculateJumpAddress();
//      if (pc.read().intValue() > 16384 && t.intValue() < 16384) {
//        return jumpAddress = WordNumber.createValue(pc.read().intValue() + 3);
//      } else {
//        return t;
//      }
//    }

    @Override
    public int execute() {
      if (positionOpcodeReference instanceof Register<T> register) {
        boolean b = condition.conditionMet(this);

        int pcValue = pc.read().intValue();
        int pointerAddress = dataflowService.findValueOrigin(register);
        if (dynamicJP.get(pcValue) == null) {
          dynamicJP.put(pcValue, new JPRegisterAddressAction.DynamicJPData(pcValue, register.read().intValue(), pointerAddress));
        }
        System.out.println("JP (HL): PC: %H, HL: %H".formatted(pcValue, register.read().intValue()));
//              Pop.doPop(memory, sp);
//              setNextPC(createValue(pc.read().intValue() + 1));
        if (lastData == null)
          return super.execute();
        else {
          setNextPC((T) lastData);
          return 0;
        }
      } else
        return super.execute();
    }

    protected String getName() {
      return "JP_";
    }

  }
}

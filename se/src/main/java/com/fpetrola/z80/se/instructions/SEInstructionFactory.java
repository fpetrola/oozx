/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.se.actions.JPRegisterAddressAction;
import com.fpetrola.z80.se.actions.SePop;
import com.fpetrola.z80.se.actions.PushReturnAddress;

import java.util.HashMap;
import java.util.Map;

public class SEInstructionFactory extends DefaultInstructionFactory {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  public static Map<java.lang.Integer, JPRegisterAddressAction.DynamicJPData> dynamicJP = new HashMap<>();
  private final DataflowService dataflowService;

  public void reset() {
    dynamicJP.clear();
    SeJP.lastData = null;
  }

  public SEInstructionFactory(SymbolicExecutionAdapter symbolicExecutionAdapter, State state, DataflowService dataflowService1) {
    super(state);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    dataflowService = dataflowService1;
  }

  public Ld Ld(OpcodeReference target, ImmutableOpcodeReference source) {
    return new Ld(target, source, flag) {
      public void execute() {
//        if (target instanceof Register register) {
//          if (register.getName().equals(RegisterName.SP.name())) {
//            symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().printStack();
//            return 0;
//          }
//        }

        if (target instanceof Register register) {
          if (register.getName().equals(RegisterName.SP.name())) {
            System.out.println("LD SP at: " + Helper.formatAddress(pc.read()));
            if (pc.read() != 0x8185) {
              int i = source.read();
              if (source instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().restoringSP(i);
              } else
                symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().changingSP(i);
            }
          }
        }

        if (source instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
          int value = source.read();
          int address = indirectMemory16BitReference.address;
          int aLU8Assign = value;
//          target.write( new DirectAccessWordNumber(aLU8Assign, pc.read(), address));
        } else if (source instanceof IndirectMemory8BitReference indirectMemory8BitReference) {
          int value = source.read();
          int address = indirectMemory8BitReference.address;
          int aLU8Assign = value;
//          target.write((T) new DirectAccessWordNumber(aLU8Assign, pc.read(), address));

        } else
          super.execute();
      }

      protected String getName() {
        return "Ld_";
      }
    };
  }

  public Ret Ret(Condition condition) {
    return new Ret(condition, sp, memory, pc) {
      public void execute() {
//            if (!getRoutineExecution().hasActionAt(getPcValue()))
//              getRoutineExecution().replaceAddressAction(new RetAddressAction(getRoutineExecution(), getPcValue()));
//            addressAction = getRoutineExecution().getActionInAddress(getPcValue());

        super.execute();
      }

      protected String getName() {
        return "Ret_";
      }
    };
  }

  public Pop Pop(OpcodeReference target) {
    return new SePop(symbolicExecutionAdapter, target, sp, memory, flag);
  }

  public Push Push(OpcodeReference target) {
    return new PushReturnAddress(symbolicExecutionAdapter, target, sp, memory);
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new SeJP(target, condition);
  }

  public Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference) {
    return new Call(positionOpcodeReference, condition, pc, sp, this.state.getMemory()) {
      public int beforeJump(int jumpAddress) {
        Integer wordNumber = pc.read();
        int value = (wordNumber + length) & 0xFFFF;
//        value = (T) new ReturnAddressWordNumber(value, pc.read());
        Push.doPush(value, sp, memory);
        return jumpAddress;
      }

      protected String getName() {
        return "Call_";
      }
    };
  }

  public class SeJP extends JP {

    public static Integer lastData;

    public SeJP(ImmutableOpcodeReference target, Condition condition) {
      super(target, condition, SEInstructionFactory.this.pc);
    }

//    @Override
//    public int calculateJumpAddress() {
//      int t = super.calculateJumpAddress();
//      if (pc.read().intValue() > 16384 && t.intValue() < 16384) {
//        return jumpAddress = WordNumber.createValue(pc.read().intValue() + 3);
//      } else {
//        return t;
//      }
//    }

    @Override
    public void execute() {
      if (positionOpcodeReference instanceof Register register) {
        boolean b = condition.conditionMet(this);

        int pcValue = pc.read();
        int pointerAddress = dataflowService.findValueOrigin(register);
        if (dynamicJP.get(pcValue) == null) {
          dynamicJP.put(pcValue, new JPRegisterAddressAction.DynamicJPData(pcValue, register.read(), pointerAddress));
        }
        System.out.println("JP (HL): PC: %H, HL: %H".formatted(pcValue, register.read()));
//              Pop.doPop(memory, sp);
//              setNextPC(createValue(pc.read().intValue() + 1));
        if (lastData == null)
          super.execute();
        else {
          setNextPC(lastData);
        }
      } else
        super.execute();
    }

    protected String getName() {
      return "JP_";
    }

  }
}

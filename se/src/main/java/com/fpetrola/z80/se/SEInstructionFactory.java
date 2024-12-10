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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.actions.PopReturnAddress;
import com.fpetrola.z80.se.actions.PushReturnAddress;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;

import java.util.HashMap;
import java.util.Map;

public class SEInstructionFactory<T extends WordNumber> extends DefaultInstructionFactory<T> {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  public static Map<Integer, DynamicJPData> dynamicJP = new HashMap<>();

  public SEInstructionFactory(SymbolicExecutionAdapter symbolicExecutionAdapter, State state) {
    super(state);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
  }

  public Ld<T> Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source) {
    return new Ld<T>(target, source, flag) {
      public int execute() {
        if (source instanceof IndirectMemory8BitReference<T>) {
          T value = source.read();
          T aLU8Assign = value;
          target.write((T) new DirectAccessWordNumber(aLU8Assign.intValue(), pc.read().intValue()));
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

  public Pop Pop(OpcodeReference target) {
    return new PopReturnAddress(symbolicExecutionAdapter, target, sp, memory, flag, pc);
  }

  public Push Push(OpcodeReference target) {
    return new PushReturnAddress(symbolicExecutionAdapter, target, sp, memory);
  }

  @Override
  public JP JP(ImmutableOpcodeReference target, Condition condition) {
    return new JP<T>(target, condition, pc) {

      @Override
      public T calculateJumpAddress() {
        T t = super.calculateJumpAddress();
        if (pc.read().intValue() > 16384 && t.intValue() < 16384) {
          return jumpAddress = WordNumber.createValue(pc.read().intValue() + 3);
        } else {
          return t;
        }
      }

      @Override
      public int execute() {
        if (positionOpcodeReference instanceof Register<T> register) {
          int pointerAddress = -1;
          VirtualComposed16BitRegister<T> virtualComposed16BitRegister = (VirtualComposed16BitRegister<T>) register;
          VirtualComposed16BitRegister<T> first = (VirtualComposed16BitRegister<T>) virtualComposed16BitRegister.getPreviousVersions().getFirst();
          Virtual8BitsRegister<T> low = (Virtual8BitsRegister<T>) first.getLow();
          Ld<T> instruction = (Ld<T>) low.instruction;
          if (instruction.getSource() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
            ImmutableOpcodeReference<T> target1 = indirectMemory16BitReference.target;
            pointerAddress = target1.read().intValue();
            System.out.println("indirectMemory16BitReference: " + target1);
          }

          dynamicJP.put(pc.read().intValue(), new DynamicJPData(pc.read().intValue(), register.read().intValue(), pointerAddress));

          System.out.println("JP (HL): PC: %H, HL: %H".formatted(pc.read().intValue(), register.read().intValue()));
//              Pop.doPop(memory, sp);
//              setNextPC(createValue(pc.read().intValue() + 1));
          T lastData = virtualComposed16BitRegister.lastData;

          if (lastData == null)
            return super.execute();
          else {
            setNextPC(lastData);
            return 0;
          }
        } else
          return super.execute();
      }

      protected String getName() {
        return "JP_";
      }

    };
  }

  public Call Call(Condition condition, ImmutableOpcodeReference positionOpcodeReference) {
    return new Call<T>(positionOpcodeReference, condition, pc, sp, this.state.getMemory()) {
      public T beforeJump(T jumpAddress) {
        T value = pc.read().plus(length);
        value = (T) new ReturnAddressWordNumber(value.intValue(), pc.read().intValue());
        Push.doPush(value, sp, memory);
        return jumpAddress;
      }

      protected String getName() {
        return "Call_";
      }
    };
  }
}

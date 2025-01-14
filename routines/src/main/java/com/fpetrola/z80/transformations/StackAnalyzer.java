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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;

import java.util.function.Function;

public class StackAnalyzer<T extends WordNumber> {
  private final State<T> state;
  private Function<StackListener, Boolean> lastEvent;
  private boolean initialized = false;
  private StackAsRepositoryState stackAsRepository = new StackAsRepositoryState();

  public StackAnalyzer(State<T> state) {
    this.state = state;
  }

  public void init() {
    Register<T> registerSP = state.getRegisterSP();
    Memory<T> memory = state.getMemory();

    int count = 0;
    int start = registerSP.read().intValue();
    while (start > 0 && count < 20) {
      T address = WordNumber.createValue(start - count);
      T t = Memory.read16Bits(memory, address);
      if (t.intValue() > 23296) {
        ReturnAddressWordNumber value = new ReturnAddressWordNumber(t.intValue(), -1);
        memory.write(address.plus1(), ((T) value).right(8));
        memory.write(address, ((T) value).and(0xFF));
      }
      count += 2;
    }
    initialized = true;
  }

  public void beforeExecution(Instruction<T> instruction) {
    int i = state.getRegisterSP().read().intValue();

    if (!initialized && i > 0xFF00)
      init();

    lastEvent = null;
    InstructionVisitor<T, Object> instructionVisitor = new InstructionVisitor<>() {
      public void visitingPop(Pop pop) {
        var read = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
        if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
          int pcValue = state.getPc().read().intValue();
          lastEvent = l -> l.returnAddressPopped(pcValue, returnAddressWordNumber.intValue(), returnAddressWordNumber.pc);
        }
      }

      public boolean visitingRet(Ret ret) {
        if (!(ret instanceof RetN) && ret.getNextPC() != null) {

          var read = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
          if (!(read instanceof ReturnAddressWordNumber)) {
            int pcValue = state.getPc().read().intValue();
            lastEvent = l -> l.jumpUsingRet(pcValue, read.intValue());
          }
          return true;
        } else return false;
      }

      public boolean visitingJP(JP<T> jp) {
        if (jp.getPositionOpcodeReference() instanceof Register<T> register) {
          int stackPlace = state.getRegisterSP().read().intValue();

          if (stackPlace >= 16384) {

            var read = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());

            if (!(read instanceof ReturnAddressWordNumber)) {
              int pcValue = state.getPc().read().intValue();
              if (Math.abs(read.intValue() - pcValue) < 20) {
                lastEvent = l -> l.simulatedCall(pcValue, read.intValue());
              }
            }
            return true;
          }
        }

        return false;
      }

      public void visitingLd(Ld<T> ld) {
        Register<T> pc = state.getPc();
        int pcValue = pc.read().intValue();

        ImmutableOpcodeReference<T> source = ld.getSource();
        OpcodeReference<T> target = ld.getTarget();

        if (source instanceof Register<T> register && register.getName().equals(RegisterName.SP.name())) {
          stackAsRepository.spReadAt = pcValue;
        }

        if (target instanceof Register<T> register && register.getName().equals(RegisterName.SP.name())) {
          if (distance(stackAsRepository.spReadAt, pcValue) < 5000) {
            System.out.println("LD SP at: " + Helper.formatAddress(pcValue));
            usingStackAsRepository(register, source, pcValue);
          }
        }
      }

      private void usingStackAsRepository(Register<T> register, ImmutableOpcodeReference<T> source, int pcValue) {
        int newSpAddress = source.read().intValue();
        int oldSpAddress = register.read().intValue();
        if (distance(oldSpAddress, newSpAddress) > 200) {
          if (!stackAsRepository.active) {
            stackAsRepository.active = true;
            stackAsRepository.lastSP = oldSpAddress;
            lastEvent = l -> l.beginUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
          } else {
            if (newSpAddress == stackAsRepository.lastSP) {
              lastEvent = l -> l.endUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
              stackAsRepository.clear();
            }
          }
        }
      }

    };
    instruction.accept(instructionVisitor);
  }

  private int distance(int oldSpAddress, int newSpAddress) {
    return Math.abs(oldSpAddress - newSpAddress);
  }

  public void afterExecution(Instruction<T> instruction) {
    InstructionVisitor<T, Object> instructionVisitor = new InstructionVisitor<>() {
      public boolean visitingCall(Call tCall) {
        if (tCall.getNextPC() != null) {
          var read = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
          T read1 = state.getPc().read();
          T value = (T) new ReturnAddressWordNumber(read.intValue(), read1.intValue());
          Memory.write16Bits(state.getMemory(), value, state.getRegisterSP().read());
        }
        return true;
      }
    };
    instruction.accept(instructionVisitor);
  }

  public boolean listenEvents(StackListener stackListener) {
    return lastEvent != null && lastEvent.apply(stackListener);
  }

  public void addExecutionListener(InstructionExecutor<T> instructionExecutor) {
    instructionExecutor.addExecutionListener(new ExecutionListener<T>() {
      public void beforeExecution(Instruction<T> instruction) {
        StackAnalyzer.this.beforeExecution(instruction);
      }

      public void afterExecution(Instruction<T> instruction) {
        StackAnalyzer.this.afterExecution(instruction);
      }
    });
  }
}

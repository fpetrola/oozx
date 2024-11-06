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

package com.fpetrola.z80.bytecode.se;

import com.fpetrola.z80.bytecode.RoutineExecution;
import com.fpetrola.z80.bytecode.Z80InstructionDriver;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.instructions.*;
import com.fpetrola.z80.instructions.base.*;
import com.fpetrola.z80.jspeccy.MutableOpcodeConditions;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemorySpy;
import com.fpetrola.z80.spy.WriteMemoryReference;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;

import java.util.*;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class SymbolicExecutionAdapter<T extends WordNumber> {
  private final State<? extends WordNumber> state;
  private final RoutineManager routineManager;
  private int lastPc;
  private int registerSP;
  private int nextSP;
  private AddressAction addressAction;
  private int minimalValidCodeAddress;
  public static Set<Integer> mutantAddress = new HashSet<>();

  public <T extends WordNumber> SymbolicExecutionAdapter(State<T> state, RoutineManager routineManager) {
    this.state = state;
    this.routineManager = routineManager;
    mutantAddress.clear();
//    state.getMemory().addMemoryWriteListener((address, value) -> {
//
//    });
  }

  public Stack<Object> stackFrames = new Stack<>();

  public Map<Integer, RoutineExecution> routineExecutions = new HashMap<>();

  public InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor<T> instructionExecutor) {
    return new DefaultInstructionFetcher<T>(state, createOpcodeConditions(state), new FetchNextOpcodeInstructionFactory(spy, state), instructionExecutor, createInstructionFactory(state));
  }

  public DefaultInstructionFactory createInstructionFactory(final State state) {
    return new DefaultInstructionFactory<T>(state) {
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
        };
      }

      public Ret Ret(Condition condition) {
        return new Ret<T>(condition, sp, memory, pc) {
          public int execute() {
            int execute = super.execute();
            return execute;
          }
        };
      }

      public Pop Pop(OpcodeReference target) {
        return new PopReturnAddress(target, sp, memory, flag, pc);
      }

      public Push Push(OpcodeReference target) {
        return new PushReturnAddress(target, sp, memory);
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
    };
  }

  public <T extends WordNumber> OpcodeConditions createOpcodeConditions(State<T> state) {
    return new MutableOpcodeConditions(state, (instruction, alwaysTrue, doBranch) -> addressAction.processBranch(doBranch, instruction, alwaysTrue, this));
  }

  public void createRoutineExecution(int jumpAddress) {
    // if (jumpAddress == 35211) System.out.println("start routine: " + jumpAddress);
    stackFrames.push(jumpAddress);
    RoutineExecution routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution(minimalValidCodeAddress));
    } else
      System.err.print("");

    routineExecution.start = jumpAddress;
  }

  public void stepUntilComplete(Z80InstructionDriver z80InstructionDriver, State<T> state, int firstAddress, int minimalValidCodeAddress) {
    this.minimalValidCodeAddress = minimalValidCodeAddress;
    memoryReadOnly(false, state);

    registerSP = state.getRegisterSP().read().intValue();

    createRoutineExecution(firstAddress);
    Register<T> pc = state.getPc();
    pc.write(createValue(firstAddress));

    executeAllCode(z80InstructionDriver, pc);

    routineExecutions.entrySet().forEach(e -> {
      if (e.getValue().actions.stream().anyMatch(AddressAction::isPending)) {
        System.err.println("");
      }
    });

    List<WriteMemoryReference> writeMemoryReferences = RegisterTransformerInstructionSpy.writeMemoryReferences;

    writeMemoryReferences.forEach(wmr -> {
      Routine routineAt = routineManager.findRoutineAt(wmr.address.intValue());
      if (routineAt != null) {
        mutantAddress.add(wmr.address.intValue());
      }
    });


    return;
  }

  private void executeAllCode(Z80InstructionDriver z80InstructionDriver, Register<T> pc) {
    boolean ready = false;
    nextSP = 0;

    while (!ready) {
      int pcValue = pc.read().intValue();
      ready = isReady(pcValue, ready);

      if (!ready) {
        RoutineExecution routineExecution = getRoutineExecution();

        addressAction = routineExecution.getActionInAddress(pcValue);
        z80InstructionDriver.step();
        addressAction.setPending(false);
        AddressAction nextAddressAction = routineExecution.getActionInAddress(pcValue);
        T value = createValue(nextAddressAction.getNext(pcValue, pc.read().intValue()));
        pc.write(value);

//        if (value.intValue() == pcValue)
//          ready= true;

        ready |= stackFrames.isEmpty();
        lastPc = pcValue;
      }
    }
  }

  private boolean isReady(int pcValue, boolean ready) {
    if (pcValue == 38243)
      System.out.print("");

    if (pcValue < minimalValidCodeAddress)
      ready = true;
    return ready;
  }

  private void checkNextSP() {
    if (nextSP == state.getRegisterSP().read().intValue()) {
      System.out.print("");
    }
  }

  public RoutineExecution getRoutineExecution() {
    return routineExecutions.get(stackFrames.peek());
  }

  protected void memoryReadOnly(boolean readOnly, State state) {
    MockedMemory<T> memory = (MockedMemory<T>) ((MemorySpy<T>) state.getMemory()).getMemory();
    memory.enableReadyOnly(readOnly);
  }

  public abstract class SymbolicInstructionFactoryDelegator implements InstructionFactoryDelegator {
    private InstructionFactory instructionFactory;

    public SymbolicInstructionFactoryDelegator() {
      this.instructionFactory = createInstructionFactory(state);
    }

    @Override
    public InstructionFactory getDelegate() {
      return instructionFactory;
    }
  }

  public class PopReturnAddress extends Pop<T> {
    public final Register<T> pc;
    public int previousPc = -1;
    public ReturnAddressWordNumber returnAddress0;
    public int popAddress;

    public ReturnAddressWordNumber getReturnAddress() {
      return returnAddress;
    }

    private ReturnAddressWordNumber returnAddress;

    public PopReturnAddress(OpcodeReference target, Register<T> sp, Memory<T> memory, Register<T> flag, Register<T> pc) {
      super(target, sp, memory, flag);
      this.pc = pc;
    }

    public int execute() {
      setNextPC(null);
      returnAddress = null;
      final T read = Memory.read16Bits(memory, sp.read());

      if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
        returnAddress0 = returnAddressWordNumber;

        // target.write(createValue(0));

        previousPc = lastPc;
        RoutineExecution routineExecution = routineExecutions.get(stackFrames.get(stackFrames.size() - 2));
        int address2 = pc.read().intValue() + 1;

        routineExecution.replaceAddressAction(new BasicAddressAction(address2));
        routineExecution.replaceAddressAction(new AddressActionDelegate(returnAddressWordNumber.intValue(), routineExecution));

        RoutineExecution lastRoutineExecution = getRoutineExecution();
        popAddress = pc.read().intValue();
        BasicAddressAction addressAction1 = new BasicAddressAction(popAddress);
        addressAction1.setPending(false);
        lastRoutineExecution.replaceAddressAction(addressAction1);
        routineExecution.replaceAddressAction(new BasicAddressAction(returnAddressWordNumber.pc) {
          @Override
          public boolean processBranch(boolean doBranch, Instruction instruction, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter) {
            if (lastRoutineExecution.hasPendingPoints()) {
              Call call = (Call) instruction;
              int jumpAddress = call.getJumpAddress().intValue();
              createRoutineExecution(jumpAddress);
              return true;
            } else {
              super.processBranch(false, instruction, alwaysTrue, symbolicExecutionAdapter);
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
          popFrame();
        }
        if (lastRoutineExecution.retInstruction == -1)
          lastRoutineExecution.retInstruction = pc.read().intValue();
      } else {
        checkNextSP();
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

    private class AddressActionDelegate extends BasicAddressAction {
      private RoutineExecution routineExecution;

      public AddressActionDelegate(int address2, RoutineExecution routineExecution) {
        super(address2);
        this.routineExecution = routineExecution;
      }

      public boolean processBranch(boolean doBranch, Instruction instruction, boolean alwaysTrue, SymbolicExecutionAdapter symbolicExecutionAdapter) {
        boolean b = super.processBranch(doBranch, instruction, alwaysTrue, symbolicExecutionAdapter);
        if (instruction instanceof ConditionalInstruction<?, ?> conditionalInstruction) {
          return routineExecution.createConditionalAction(conditionalInstruction, address).processBranch(doBranch, instruction, alwaysTrue, symbolicExecutionAdapter);
        } else {
          return b;
        }
      }

      @Override
      public int getNext(int next, int pcValue) {
        return super.getNext(next, pcValue);
      }
    }
  }

  public void popFrame() {
    stackFrames.pop();
  }

  public class PushReturnAddress extends Push<T> {
    public PushReturnAddress(OpcodeReference target, Register<T> sp, Memory<T> memory) {
      super(target, sp, memory);
    }

    public int execute() {
      doPush(createValue(target.read().intValue()), sp, memory);
      checkNextSP();

      return 5 + cyclesCost;
    }

    protected String getName() {
      return "Push_";
    }
  }
}































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

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactoryDelegator;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.actions.AddressAction;
import com.fpetrola.z80.se.actions.JPRegisterAddressAction;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemorySpy;
import com.fpetrola.z80.spy.WriteMemoryReference;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;

import java.util.*;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class SymbolicExecutionAdapter<T extends WordNumber> {
  public Stack<Object> stackFrames = new Stack<>();
  public Map<Integer, RoutineExecution> routineExecutions = new HashMap<>();
  private final State<? extends WordNumber> state;
  private final RoutineManager routineManager;
  private final RoutineFinderInstructionSpy spy;
  public int lastPc;
  private int registerSP;
  private int nextSP;
  private AddressAction addressAction;
  private Z80InstructionDriver z80InstructionDriver;
  private int minimalValidCodeAddress;
  private Set<Integer> mutantAddress = new HashSet<>();
  private Register<T> pc;
  private DataflowService dataflowService;

  public int getPcValue() {
    return pc.read().intValue();
  }

  public void reset() {
    mutantAddress.clear();
    stackFrames.clear();
    routineExecutions.clear();
    routineManager.reset();
    spy.reset(state);
    nextSP = 0;
    lastPc = 0;
    addressAction = null;
  }

  public <T extends WordNumber> SymbolicExecutionAdapter(State<T> state, RoutineManager routineManager, RoutineFinderInstructionSpy spy, DataflowService dataflowService1) {
    this.state = state;
    this.routineManager = routineManager;
    this.spy = spy;
    this.spy.addExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
//        System.out.println(instruction);
      }

      public void afterExecution(Instruction instruction) {
//        System.out.println(instruction);

      }
    });
    mutantAddress.clear();
    dataflowService = dataflowService1;
  }

  public InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor<T> instructionExecutor, OpcodeConditions opcodeConditions) {
    return new DefaultInstructionFetcher<T>(state, opcodeConditions, new FetchNextOpcodeInstructionFactory(spy, state), instructionExecutor, createInstructionFactory(state), true, true);
  }

  public DefaultInstructionFactory createInstructionFactory(final State state) {
    return new SEInstructionFactory(this, state, dataflowService);
  }

  public <T extends WordNumber> MutableOpcodeConditions createOpcodeConditions(State<T> state) {
    return new MutableOpcodeConditions(state, (instruction, alwaysTrue, doBranch) -> {
      addressAction = getRoutineExecution().replaceIfAbsent(getPcValue(), getRoutineExecution().createAddressAction(instruction, alwaysTrue, getPcValue(), this, state));
      return addressAction.processBranch(instruction);
    });
  }

  public void createRoutineExecution(int jumpAddress) {
    // if (jumpAddress == 35211) System.out.println("start routine: " + jumpAddress);
    if (jumpAddress == 0xCFD9)
      System.out.println("");
    stackFrames.push(jumpAddress);
    RoutineExecution routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution(minimalValidCodeAddress, this));
    } else
      System.err.print("");

    routineExecution.start = jumpAddress;
  }

  public void stepUntilComplete(Z80InstructionDriver z80InstructionDriver, State<T> state, int firstAddress, int minimalValidCodeAddress) {
    stepAllAndProcessPending(z80InstructionDriver, state, firstAddress, minimalValidCodeAddress);
    processPending();
    routineManager.optimizeAllSplit();
  }

  private void stepAllAndProcessPending(Z80InstructionDriver z80InstructionDriver, State<T> state, int firstAddress, int minimalValidCodeAddress) {
    this.z80InstructionDriver = z80InstructionDriver;
    this.minimalValidCodeAddress = minimalValidCodeAddress;
    memoryReadOnly(false, state);

    registerSP = state.getRegisterSP().read().intValue();

    createRoutineExecution(firstAddress);
    pc = state.getPc();
    pc.write(createValue(firstAddress));

    executeAllCode(z80InstructionDriver, pc);

    List<WriteMemoryReference> writeMemoryReferences = spy.getWriteMemoryReferences();

    SEInstructionFactory.dynamicJP.forEach((pc, dj) -> {
      for (int j = 0; j < writeMemoryReferences.size(); j++) {
        WriteMemoryReference w = writeMemoryReferences.get(j);
        if (w.address.intValue() == dj.pointerAddress()) {
          WriteMemoryReference w2 = writeMemoryReferences.get(j + 1);
          if (w2.address.intValue() == dj.pointerAddress() + 1) {
            int value = w2.value.intValue() * 256 + w.value.intValue();
            dj.addCase(value);
          }
        }
      }
    });
    writeMemoryReferences.forEach(wmr -> {
      Routine routineAt = routineManager.findRoutineAt(wmr.address.intValue());
      if (routineAt != null) {
        mutantAddress.add(wmr.address.intValue());
      }
    });
  }

  private void processPending() {
    Map<Integer, RoutineExecution> routineExecutions1 = new HashMap<>(routineExecutions);
    routineExecutions1.entrySet().forEach(e -> {
      if (e.getValue().actions.stream().anyMatch(AddressAction::isPending)) {
        System.err.println("pending action: " + Helper.formatAddress(e.getValue().start));
      }
      Optional<AddressAction> first = e.getValue().actions.stream().filter(addressAction1 -> addressAction1 instanceof JPRegisterAddressAction jpRegisterAddressAction).findFirst();
      if (first.isPresent()) {
        JPRegisterAddressAction jpRegisterAddressAction = (JPRegisterAddressAction) first.get();
        if (jpRegisterAddressAction.dynamicJPData == null) {
          DynamicJPData dynamicJPData = SEInstructionFactory.dynamicJP.get(jpRegisterAddressAction.address);
          jpRegisterAddressAction.setDynamicJPData(dynamicJPData);
          List<Integer> integers = routineManager.callers2.get(e.getValue().start);
          Integer first1 = integers.getFirst();
          int startAddress = first1;
          pushAddress(startAddress); //FiXME: calculate minimal ret to run
          pushAddress(startAddress);
          pushAddress(startAddress);
          pushAddress(startAddress);
          stepAllAndProcessPending(z80InstructionDriver, (State<T>) state, first1, minimalValidCodeAddress);
        }
      }
    });
  }

  private void pushAddress(int startAddress) {
    Register<WordNumber> registerSP1 = (Register<WordNumber>) state.getRegisterSP();
    Memory<WordNumber> memory = (Memory<WordNumber>) state.getMemory();
    Push.doPush(createValue(startAddress), registerSP1, memory);
  }

  private void executeAllCode(Z80InstructionDriver z80InstructionDriver, Register<T> pc) {
    boolean ready = false;
    nextSP = 0;

    while (!ready) {
      int pcValue = pc.read().intValue();
      ready = isReady(pcValue, ready);

      if (!ready) {
        RoutineExecution routineExecution = getRoutineExecution();

        addressAction = routineExecution.getAddressAction(pcValue);
        if (addressAction != null) {
          pcValue = addressAction.getNextPC();
          pc.write(createValue(pcValue));
        }

//        System.out.println("PC: " + Helper.formatAddress(pcValue));
//        System.out.println("BC: " + Helper.formatAddress(state.getRegister(RegisterName.BC).read().intValue()));

        if (pcValue == 0xE9BC)
          System.out.println("");

        AddressAction currentAddressAction = routineExecution.getAddressAction(pcValue);
        if (currentAddressAction != null)
          currentAddressAction.beforeStep();

        z80InstructionDriver.step();

        if (!routineExecution.hasActionAt(pcValue))
          addressAction = routineExecution.getActionInAddress(pcValue);

        AddressAction nextAddressAction = routineExecution.getActionInAddress(pcValue);
        nextAddressAction.setPendingAfterStep(this);
        T value = createValue(nextAddressAction.getNext(pcValue, pc.read().intValue()));
        pc.write(value);

//        if (value.intValue() == pcValue)
//          ready= true;

        ready |= stackFrames.isEmpty();
        lastPc = pcValue;
        routineExecution.lastPc = pcValue;

        addressAction = null;
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

  public void checkNextSP() {
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

  public Set<Integer> getMutantAddress() {
    return mutantAddress;
  }

  public abstract class SymbolicInstructionFactoryDelegator implements InstructionFactoryDelegator {
    private final InstructionFactory instructionFactory;

    public SymbolicInstructionFactoryDelegator() {
      this.instructionFactory = createInstructionFactory(state);
    }

    @Override
    public InstructionFactory getDelegate() {
      return instructionFactory;
    }
  }

  public void popFrame() {
    stackFrames.pop();
  }

}































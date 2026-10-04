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

import com.fpetrola.z80.cpu.CachedInstructionFetcher;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactoryDelegator;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.actions.*;
import com.fpetrola.z80.se.instructions.SEInstructionFactory;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.spy.WriteMemoryReference;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;
import com.fpetrola.z80.transformations.StackAnalyzer;

import java.util.*;

public class SymbolicExecutionAdapter {
  public final State state;
  private final RoutineManager routineManager;
  private final RoutineFinderInstructionSpy spy;
  public final RoutineExecutorHandler routineExecutorHandler;
  public int lastPc;
  private int registerSP;
  private int nextSP;
  private Z80InstructionDriver z80InstructionDriver;
  private int minimalValidCodeAddress;
  private Set<Integer> mutantAddress = new HashSet<>();
  private Register pc;
  private DataflowService dataflowService;
  private SEInstructionFactory sEInstructionFactory;

  public StackAnalyzer getStackAnalyzer() {
    return stackAnalyzer;
  }

  private StackAnalyzer stackAnalyzer;
  private final RoutineFinder routineFinder;
  private final InstructionExecutor instructionExecutor;

  public SymbolicExecutionAdapter(State state, RoutineManager routineManager, RoutineFinderInstructionSpy spy, DataflowService dataflowService1, StackAnalyzer stackAnalyzer, RoutineFinder routineFinder, InstructionExecutor instructionExecutor) {
    this.state = state;
    this.routineManager = routineManager;
    this.spy = spy;
    this.stackAnalyzer = stackAnalyzer;
    this.routineFinder = routineFinder;
    this.instructionExecutor = instructionExecutor;
    mutantAddress.clear();
    dataflowService = dataflowService1;
    routineExecutorHandler = new RoutineExecutorHandler(state, new ExecutionStackStorage(state, stackAnalyzer), dataflowService, stackAnalyzer);
    this.stackAnalyzer.addEventListener(new StackListener() {
      public boolean jumpUsingRet(int pcValue, Set<Integer> jumpAddresses) {
        AddressAction addressAction = routineExecutorHandler.getCurrentRoutineExecution().getAddressAction(pcValue);
        if (!(addressAction instanceof JumpUsingRetAddressAction))
          routineExecutorHandler.getCurrentRoutineExecution().replaceAddressAction(new JumpUsingRetAddressAction(pcValue, jumpAddresses, routineExecutorHandler));
        return StackListener.super.jumpUsingRet(pcValue, jumpAddresses);
      }
    });

    instructionExecutor.addTopExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
        int pcValue = state.getPc().read();

        RoutineExecution currentRoutineExecution = routineExecutorHandler.getCurrentRoutineExecution();
        AddressAction addressAction = currentRoutineExecution.getAddressAction(pcValue);
        if (addressAction == null) {
          RoutineExecution routineExecution = routineExecutorHandler.getCurrentRoutineExecution();
          if (instruction instanceof ConditionalInstruction<?> conditionalInstruction) {
            addressAction = routineExecution.replaceIfAbsent(getPcValue(), routineExecution.createAddressAction(instruction, conditionalInstruction.getCondition() instanceof ConditionAlwaysTrue, getPcValue()));
          } else
            addressAction = routineExecution.createAndAddGenericAction(pcValue);
        }

        if (instruction instanceof ConditionalInstruction<?>) {
          ExecutionStackStorage executionStackStorage = addressAction.getExecutionStackStorage();
          if (executionStackStorage.isSaved())
            executionStackStorage.restore();
          else
            executionStackStorage.save();
        }
      }

      public void afterExecution(Instruction instruction) {
      }
    });
  }

  public int getPcValue() {
    return pc.read();
  }

  public void reset() {
    mutantAddress.clear();
    routineExecutorHandler.reset();
    routineManager.reset();
    spy.reset(state);
    nextSP = 0;
    lastPc = 0;
    sEInstructionFactory.reset();
    routineFinder.reset();
  }

  public InstructionFetcher createInstructionFetcher(State state, OpcodeConditions opcodeConditions) {
    return new CachedInstructionFetcher(state, opcodeConditions, createInstructionFactory(state), true);
  }

  public InstructionFactory createInstructionFactory(final State state) {
    return sEInstructionFactory = new SEInstructionFactory(this, state, dataflowService);
  }

  public  MutableOpcodeConditions createOpcodeConditions(State state) {
    return new MutableOpcodeConditions(state, (instruction, alwaysTrue, doBranch) -> {
//      System.out.printf("pc: %s -> %s%n", Helper.formatAddress(getPcValue()), instruction);
      return routineExecutorHandler.getCurrentRoutineExecution().getAddressAction(getPcValue()).processBranch(instruction);
    });
  }

  public void stepUntilComplete(Z80InstructionDriver z80InstructionDriver, State state, int firstAddress, int minimalValidCodeAddress) {
    stepAllAndProcessPending(z80InstructionDriver, state, firstAddress, minimalValidCodeAddress);
    routineFinder.attributeUnclaimedCode();
    routineManager.createVirtualRoutines();
  }

  private void stepAllAndProcessPending(Z80InstructionDriver z80InstructionDriver, State state, int firstAddress, int minimalValidCodeAddress) {
    this.z80InstructionDriver = z80InstructionDriver;
    this.minimalValidCodeAddress = minimalValidCodeAddress;
    routineFinder.reset();
    memoryReadOnly(false, state);

    registerSP = state.getRegisterSP().read();

    routineExecutorHandler.createRoutineExecution(firstAddress);
    pc = state.getPc();
    updatePcRegister(firstAddress);

    executeAllCode(z80InstructionDriver, pc);

//    processPending();
    checkPending();
    List<WriteMemoryReference> writeMemoryReferences = spy.getWriteMemoryReferences();

    findMutantCode(writeMemoryReferences);
  }

  private void findMutantCode(List<WriteMemoryReference> writeMemoryReferences) {
    writeMemoryReferences.forEach(wmr -> {
      Routine routineAt = routineManager.findRoutineAt(wmr.address);
      if (routineAt != null) {
        if (wmr.address == 0xb894)
          System.out.println("asasgsag");
        mutantAddress.add(wmr.address);
      }
    });
  }

  private void executeAllCode(Z80InstructionDriver z80InstructionDriver, Register pc) {
    var ready = false;
    nextSP = 0;

    while (!ready) {
      var pcValue = pc.read();
      ready = isReady(pcValue);

      if (pcValue == 34493)
        System.out.println("aca!");
      if (!ready) {
        var routineExecution = routineExecutorHandler.getCurrentRoutineExecution();

        var addressAction = routineExecution.getAddressAction(pcValue);
        if (addressAction != null)
          pcValue = updatePcRegister(addressAction.getNextPC());

//        routineExecutorHandler.getExecutionStackStorage().printStack();

        if (pcValue == -1)
          ready = true;
        else {
          z80InstructionDriver.step();
          this.stackAnalyzer.listenEvents(new SEStackListener(this));

//          routineExecutorHandler.getExecutionStackStorage().printStack();

          updatePcRegister(routineExecution.getAddressAction(pcValue).getNext(pcValue, pc.read()));

          ready = routineExecutorHandler.isEmpty();
        }
        lastPc = pcValue;
      }
    }
  }

  private int updatePcRegister(int pcValue) {
    logPC(pcValue);
    pc.write(pcValue);
    return pcValue;
  }

  private void logPC(int pcValue) {
//    System.out.println("PC: " + Helper.formatAddress(pcValue));
//        System.out.println("BC: " + Helper.formatAddress(state.getRegister(RegisterName.BC).read()));
  }

  private boolean isReady(int pcValue) {
    if (pcValue < minimalValidCodeAddress)
      return true;
    return false;
  }

  public void checkNextSP() {
    if (nextSP == state.getRegisterSP().read()) {
      System.out.print("");
    }
  }

  private void executingPending(int address) {
    RoutineExecution routineExecutionAt = routineExecutorHandler.findRoutineExecutionContaining(address);
    routineExecutorHandler.pushRoutineExecution(routineExecutionAt);
    pc.write(address);
    executeAllCode(z80InstructionDriver, pc);
  }

  private void processPending() {
    Map<Integer, RoutineExecution> routineExecutions1 = routineExecutorHandler.getCopyListOfRoutineExecutions();
    routineExecutions1.entrySet().forEach(e -> {
      if (e.getValue().hasPendingPoints()) {
        executingPending(e.getValue().getStart());
      }
    });
  }

  private void checkPending() {
    Map<Integer, RoutineExecution> routineExecutions1 = routineExecutorHandler.getCopyListOfRoutineExecutions();
    routineExecutions1.entrySet().forEach(e -> {
      if (e.getValue().hasPendingPoints()) {
        System.err.println("pending action: " + e.getValue());
      }
    });
  }

  protected void memoryReadOnly(boolean readOnly, State state) {
    MockedMemory memory = (MockedMemory) state.getMemory();
    memory.enableReadyOnly(readOnly);
  }

  public Set<Integer> getMutantAddress() {
    return mutantAddress;
  }

  private static class SEStackListener implements StackListener {
    private final SymbolicExecutionAdapter symbolicExecutionAdapter;

    private SEStackListener(SymbolicExecutionAdapter symbolicExecutionAdapter) {
      this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    }

    public boolean returnAddressPopped(int pcValue, int returnAddress, int callAddress) {
      RoutineExecutorHandler routineExecutorHandler = symbolicExecutionAdapter.routineExecutorHandler;

      var lastRoutineExecution = routineExecutorHandler.getCurrentRoutineExecution();
      var callerRoutineExecution = routineExecutorHandler.getCallerRoutineExecution();

      callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(pcValue + 1, routineExecutorHandler));
      callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(returnAddress, routineExecutorHandler));
      lastRoutineExecution.replaceAddressAction(new BasicAddressAction(pcValue, routineExecutorHandler, false));
      callerRoutineExecution.replaceAddressAction(new PopReturnCallAddressAction(routineExecutorHandler, lastRoutineExecution, callAddress));

      routineExecutorHandler.popRoutineExecution();
      if (!lastRoutineExecution.hasRetInstruction())
        lastRoutineExecution.setRetInstruction(pcValue);
      return true;
    }

    public boolean beginUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
      symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().disable();
      return StackListener.super.beginUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
    }

    @Override
    public boolean endUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
      symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().enable();
      return StackListener.super.endUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
    }

    @Override
    public boolean droppingReturnValues(int pcValue, int newSpAddress, int oldSpAddress, StackAnalyzer.Entry lastReturnAddress) {
      RoutineExecutorHandler routineExecutorHandler = symbolicExecutionAdapter.routineExecutorHandler;

      if (lastReturnAddress != null) {
        var lastRoutineExecution = routineExecutorHandler.getCurrentRoutineExecution();
        var callerRoutineExecution = routineExecutorHandler.findRoutineExecutionContaining(lastReturnAddress.pc());


        RoutineExecution popRoutine = lastRoutineExecution;
        List<RoutineExecution> stackedRoutines = new ArrayList<>();
        while (popRoutine != callerRoutineExecution) {
          stackedRoutines.add(popRoutine);
          int start = routineExecutorHandler.popRoutineExecution();
          popRoutine = routineExecutorHandler.getCurrentRoutineExecution();
        }

        callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(symbolicExecutionAdapter.routineManager.addressAfter(pcValue), routineExecutorHandler));
        callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(lastReturnAddress.value(), routineExecutorHandler));
        lastRoutineExecution.replaceAddressAction(new BasicAddressAction(pcValue, routineExecutorHandler, false));
//        if (callerRoutineExecution.getAddressAction(lastReturnAddress.pc) instanceof ChangeStackCallAddressAction changeStackCallAddressAction) {
//          changeStackCallAddressAction.getLastRoutineExecutions().addAll(stackedRoutines);
//        } else
        callerRoutineExecution.replaceAddressAction(new ChangeStackCallAddressAction(routineExecutorHandler, stackedRoutines, lastReturnAddress.pc()));

        if (!lastRoutineExecution.hasRetInstruction())
          lastRoutineExecution.setRetInstruction(pcValue);
        return true;
      } else
        return StackListener.super.droppingReturnValues(pcValue, newSpAddress, oldSpAddress, lastReturnAddress);
    }

    public boolean jumpUsingRet(int pcValue, Set<Integer> jumpAddresses) {
      return StackListener.super.jumpUsingRet(pcValue, jumpAddresses);
    }

    public boolean simulatedCall(int pcValue, int jumpAddress, Set<Integer> jumpAddresses, int returnAddress) {
      return StackListener.super.simulatedCall(pcValue, jumpAddress, jumpAddresses, returnAddress);
    }
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
}































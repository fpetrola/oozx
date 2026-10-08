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
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.se.actions.RetAddressAction;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
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
  private Z80InstructionDriver z80InstructionDriver;
  private int explorationSP = -1;
  private boolean returnedToUnknownAddress;
  private static final int CALLER_STACK = 64;
  private final List<int[]> protectedCallerStacks = new ArrayList<>();

  private void protectCallerStack(int sp) {
    int[] range = {sp, Math.min(sp + CALLER_STACK, 0x10000)};
    protectedCallerStacks.add(range);
    state.getMemory().protect(range[0], range[1]);
  }

  private void unprotectCallerStack(int sp) {
    protectedCallerStacks.removeIf(range -> range[0] == sp && unprotected(range));
  }

  private boolean unprotected(int[] range) {
    state.getMemory().unprotect(range[0], range[1]);
    return true;
  }
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
    routineExecutorHandler = new RoutineExecutorHandler(state, routineManager, new ExecutionStackStorage(state, stackAnalyzer), dataflowService, stackAnalyzer);
    this.stackAnalyzer.addEventListener(new StackListener() {
      public boolean jumpUsingRet(Ret ret, int pcValue, Set<Integer> jumpAddresses) {
        AddressAction addressAction = routineExecutorHandler.getCurrentRoutineExecution().getAddressAction(pcValue);
        if (!(addressAction instanceof JumpUsingRetAddressAction))
          routineExecutorHandler.getCurrentRoutineExecution().replaceAddressAction(new JumpUsingRetAddressAction(ret, pcValue, jumpAddresses, routineExecutorHandler));
        return StackListener.super.jumpUsingRet(ret, pcValue, jumpAddresses);
      }

      public boolean returnShifted(Instruction instruction, int pcValue, int returnAddress, int callSite) {
        if (instruction instanceof JP jp) {
          ((Register) jp.getPositionOpcodeReference()).write(returnAddress);
          routineExecutorHandler.getCurrentRoutineExecution().replaceAddressAction(new RetAddressAction(instruction, pcValue, true, routineExecutorHandler));
        } else
          state.getMemory().write16Bits(returnAddress, state.getRegisterSP().read());
        return StackListener.super.returnShifted(instruction, pcValue, returnAddress, callSite);
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
          if (addressAction.takeResuming() && executionStackStorage.isSaved())
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
    state.getMemory().unprotect(0, 0x10000);
    stackAnalyzer.forgetLearned();
    explorationSP = -1;
    routineExecutorHandler.reset();
    routineManager.reset();
    spy.reset(state);
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
    routineManager.setCodeStart(minimalValidCodeAddress);
    routineFinder.reset();
    memoryReadOnly(false, state);

    if (explorationSP == -1)
      explorationSP = state.getRegisterSP().read();
    state.getRegisterSP().write(explorationSP);
    stackAnalyzer.forgetStack();
    protectedCallerStacks.forEach(range -> state.getMemory().unprotect(range[0], range[1]));
    protectedCallerStacks.clear();
    routineExecutorHandler.getExecutionStackStorage().newExploration();
    routineExecutorHandler.newExploration();
    routineExecutorHandler.getStackFrames().clear();

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
    java.util.stream.Stream.concat(writeMemoryReferences.stream().map(wmr -> wmr.address), routineManager.fixedStoreTargets(state.getMemory().getData())).distinct()
        .filter(address -> !mutantAddress.contains(address) && !stackAnalyzer.codeVersions.inBlock(address) && routineManager.originalAddress(address) == address && routineManager.findRoutineAt(address) != null)
        .forEach(mutantAddress::add);
  }


  private void executeAllCode(Z80InstructionDriver z80InstructionDriver, Register pc) {

    for (long steps = 0; !routineExecutorHandler.isEmpty(); steps++) {
      if (steps == 1_000_000) {
        System.out.println("exploration abandoned at " + Helper.formatAddress(pc.read()) + " after " + steps + " steps");
        routineExecutorHandler.getStackFrames().clear();
        return;
      }
      var pcValue = pc.read();
      var routineExecution = routineExecutorHandler.getCurrentRoutineExecution();

      if (!routineManager.isCode(pcValue))
        pcValue = updatePcRegister(routineExecution.getNextPending().address);
      else {
        var addressAction = routineExecution.getAddressAction(pcValue);
        if (addressAction != null)
          pcValue = updatePcRegister(addressAction.getNextPC());
      }

      if (pcValue == -1)
        unwindFullyExploredRoutine();
      else {
        z80InstructionDriver.step();
        returnedToUnknownAddress = false;
        this.stackAnalyzer.listenEvents(new SEStackListener(this));

        int next = routineExecution.getAddressAction(pcValue).getNext(pcValue, pc.read());
        if (returnedToUnknownAddress && pc.read() != pcValue + 1 && !routineExecutorHandler.isEmpty())
          next = routineExecutorHandler.getCurrentRoutineExecution().getNextPending().address;
        if (isTailCallToRom(pcValue)) {
          routineExecution.setRetInstruction(pcValue);
          next = routineExecution.hasPendingPoints() ? routineExecution.getNextPending().address : returnFromRom();
        }
        updatePcRegister(next);
        lastPc = pcValue;
      }
    }
  }


  private boolean isTailCallToRom(int pcValue) {
    return routineManager.getInstructionAt(pcValue) instanceof JP jp && jp.getCondition() instanceof ConditionAlwaysTrue && !routineManager.isCode(jp.getJumpAddress());
  }

  private int returnFromRom() {
    Register sp = state.getRegisterSP();
    int returnAddress = state.getMemory().read16Bits(sp.read());
    sp.write(sp.read() + 2 & 0xffff);
    routineExecutorHandler.popRoutineExecution();
    routineFinder.returnedTo(returnAddress);
    return returnAddress;
  }

  private void unwindFullyExploredRoutine() {
    routineExecutorHandler.popRoutineExecution();
    if (!routineExecutorHandler.isEmpty())
      updatePcRegister(routineExecutorHandler.getCurrentRoutineExecution().getNextPending().address);
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

    public boolean returningToUnknownAddress(int pcValue) {
      symbolicExecutionAdapter.returnedToUnknownAddress = true;
      return true;
    }

    public boolean returnAddressPopped(int pcValue, int returnAddress, int callAddress) {
      RoutineExecutorHandler routineExecutorHandler = symbolicExecutionAdapter.routineExecutorHandler;
      RoutineManager routineManager = symbolicExecutionAdapter.routineManager;
      if (symbolicExecutionAdapter.stackAnalyzer.callContinuations.containsKey(callAddress))
        return false;
      if (routineExecutorHandler.getStackFrames().size() < 2 || !routineManager.isCalledFrom(routineManager.findRoutineAt(routineExecutorHandler.getCurrentRoutineExecution().getStart()), callAddress))
        return false;

      var lastRoutineExecution = routineExecutorHandler.getCurrentRoutineExecution();
      var callerRoutineExecution = routineExecutorHandler.getCallerRoutineExecution();

      callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(pcValue + 1, routineExecutorHandler));
      if (symbolicExecutionAdapter.routineManager.isCode(returnAddress))
        callerRoutineExecution.replaceAddressAction(new AddressActionDelegate(returnAddress, routineExecutorHandler));
      lastRoutineExecution.replaceAddressAction(new BasicAddressAction(pcValue, routineExecutorHandler, false));
      callerRoutineExecution.replaceAddressAction(new PopReturnCallAddressAction(routineExecutorHandler, lastRoutineExecution, callAddress));

      routineExecutorHandler.popRoutineExecution();
      if (!lastRoutineExecution.hasRetInstruction())
        lastRoutineExecution.setRetInstruction(pcValue);
      return true;
    }

    public boolean beginUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
      if ((newSpAddress - 2 - oldSpAddress & 0xffff) >= CALLER_STACK) {
        symbolicExecutionAdapter.protectCallerStack(oldSpAddress);
        symbolicExecutionAdapter.routineExecutorHandler.getExecutionStackStorage().disable();
      }
      return StackListener.super.beginUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
    }

    @Override
    public boolean endUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
      symbolicExecutionAdapter.unprotectCallerStack(newSpAddress);
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































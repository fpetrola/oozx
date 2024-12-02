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
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemorySpy;
import com.fpetrola.z80.spy.WriteMemoryReference;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;

import java.util.*;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class SymbolicExecutionAdapter<T extends WordNumber> {
  public Stack<Object> stackFrames = new Stack<>();
  public Map<Integer, RoutineExecution> routineExecutions = new HashMap<>();
  private final State<? extends WordNumber> state;
  private final RoutineManager routineManager;
  private final RegisterTransformerInstructionSpy spy;
  public int lastPc;
  private int registerSP;
  private int nextSP;
  private AddressAction addressAction;
  private int minimalValidCodeAddress;
  private Set<Integer> mutantAddress = new HashSet<>();
  private Register<T> pc;

  public int getPcValue() {
    return pc.read().intValue();
  }

  public void reset() {
    mutantAddress.clear();
    stackFrames.clear();
    routineExecutions.clear();
    nextSP = 0;
    lastPc = 0;
    addressAction = null;
  }

  public <T extends WordNumber> SymbolicExecutionAdapter(State<T> state, RoutineManager routineManager, RegisterTransformerInstructionSpy spy) {
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
  }

  public InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor<T> instructionExecutor, OpcodeConditions opcodeConditions) {
    return new DefaultInstructionFetcher<T>(state, opcodeConditions, new FetchNextOpcodeInstructionFactory(spy, state), instructionExecutor, createInstructionFactory(state));
  }

  public DefaultInstructionFactory createInstructionFactory(final State state) {
    return new SEInstructionFactory(this, state);
  }

  public <T extends WordNumber> OpcodeConditions createOpcodeConditions(State<T> state) {
    return new MutableOpcodeConditions(state, (instruction, alwaysTrue, doBranch) -> {
      addressAction = getRoutineExecution().replaceIfAbsent(getPcValue(), getRoutineExecution().createAddressAction(instruction, alwaysTrue, getPcValue(), this));
      return addressAction.processBranch(instruction);
    });
  }

  public void createRoutineExecution(int jumpAddress) {
    // if (jumpAddress == 35211) System.out.println("start routine: " + jumpAddress);
    stackFrames.push(jumpAddress);
    RoutineExecution routineExecution = routineExecutions.get(jumpAddress);
    if (routineExecution == null) {
      routineExecutions.put(jumpAddress, routineExecution = new RoutineExecution(minimalValidCodeAddress, this));
    } else
      System.err.print("");

    routineExecution.start = jumpAddress;
  }

  public void stepUntilComplete(Z80InstructionDriver z80InstructionDriver, State<T> state, int firstAddress, int minimalValidCodeAddress) {
    this.minimalValidCodeAddress = minimalValidCodeAddress;
    memoryReadOnly(false, state);

    registerSP = state.getRegisterSP().read().intValue();

    createRoutineExecution(firstAddress);
    pc = state.getPc();
    pc.write(createValue(firstAddress));

    executeAllCode(z80InstructionDriver, pc);

    routineExecutions.entrySet().forEach(e -> {
      if (e.getValue().actions.stream().anyMatch(AddressAction::isPending)) {
        System.err.println();
      }
    });

    List<WriteMemoryReference> writeMemoryReferences = spy.getWriteMemoryReferences();

    printJPHL(writeMemoryReferences, 0xCFCA);
    printJPHL(writeMemoryReferences, 0xE9BA);
    printJPHL(writeMemoryReferences, 0xDA8B);
    writeMemoryReferences.forEach(wmr -> {
      Routine routineAt = routineManager.findRoutineAt(wmr.address.intValue());
      if (routineAt != null) {
        mutantAddress.add(wmr.address.intValue());
      }
    });
  }

  private void printJPHL(List<WriteMemoryReference> writeMemoryReferences, int i) {
    for (int j = 0; j < writeMemoryReferences.size(); j++) {
      WriteMemoryReference w = writeMemoryReferences.get(j);
      if (w.address.intValue() == i) {
        WriteMemoryReference w2 = writeMemoryReferences.get(j + 1);

        int value = w2.value.intValue() * 256 + w.value.intValue();
        System.out.println("0x" + Helper.formatAddress(i) + ":  " +  Helper.formatAddress(value));
      }
    }
  }

  private void executeAllCode(Z80InstructionDriver z80InstructionDriver, Register<T> pc) {
    boolean ready = false;
    nextSP = 0;

    while (!ready) {
      int pcValue = pc.read().intValue();
      ready = isReady(pcValue, ready);
//      if (pcValue == 0xCEC3)
//        System.out.println("ddgsdggd");

      if (!ready) {
        RoutineExecution routineExecution = getRoutineExecution();

        addressAction = routineExecution.getAddressAction(pcValue);
        if (addressAction != null) {
          pcValue = addressAction.getNextPC();
          pc.write(createValue(pcValue));
        }

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

        addressAction = null;
//        System.out.println("PC: " + Helper.formatAddress(pcValue));
//        System.out.println("BC: " + Helper.formatAddress(state.getRegister(RegisterName.BC).read().intValue()));

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































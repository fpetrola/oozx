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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.instructions.cache.InstructionCloner;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class DefaultInstructionFetcher<T extends WordNumber> implements InstructionFetcher {
  private final InstructionFactory instructionFactory;
  protected State<T> state;
  protected Instruction<T>[] opcodesTables;
  protected T pcValue;
  protected final InstructionExecutor<T> instructionExecutor;
  protected Supplier<TableBasedOpCodeDecoder> tableFactory;
  public Instruction<T> currentInstruction;

  @Override
  public boolean isNoRepeat() {
    return noRepeat;
  }

  private boolean noRepeat;
  private boolean clone;
  private List<FetchListener> fetchListeners = new ArrayList<>();
  private int prefetchPC = -1;
  private Instruction<T> prefetchedInstruction;
  private int rdelta;
  private boolean prefetch = false;
  private Register<T> registerR;
  private Memory<T> memory;

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat, boolean clone, boolean prefetch) {
    this.state = aState;
    this.instructionExecutor = instructionExecutor;
    this.noRepeat = noRepeat;
    this.prefetch = prefetch;
    tableFactory = () -> createOpcodesTables(opcodeConditions, instructionFactory.getFetchNextOpcodeInstructionFactory(), instructionFactory);
    createOpcodeTables();
    pcValue = state.getPc().read();
    this.instructionFactory = instructionFactory;
    this.clone = clone;
    this.registerR = state.getRegisterR();
    this.memory = state.getMemory();
  }

  public DefaultInstructionFetcher(State aState, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), instructionExecutor, instructionFactory, noRepeat, clone, prefetch);
  }

  public DefaultInstructionFetcher(State aState, boolean noRepeat, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), new DefaultInstructionExecutor<>(aState), new DefaultInstructionFactory(aState), noRepeat, clone, prefetch);
  }

  public DefaultInstructionFetcher(State aState, InstructionExecutor<T> instructionExecutor, boolean noRepeat, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), instructionExecutor, new DefaultInstructionFactory(aState), noRepeat, clone, prefetch);
  }

  protected void createOpcodeTables() {
    opcodesTables = tableFactory.get().getOpcodeLookupTable();
  }

  public TableBasedOpCodeDecoder createOpcodesTables(OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionFactory instructionFactory) {
    return new TableBasedOpCodeDecoder<T>(this.state, opcodeConditions, fetchInstructionFactory, instructionFactory);
  }

  @Override
  public Instruction<T> fetchNextInstruction() {
    registerR.increment();
    pcValue = state.getPc().read();

    if (prefetchPC != pcValue.intValue()) {
      currentInstruction = fetchInstruction(pcValue);
      prefetchedInstruction = currentInstruction;
    } else {
      currentInstruction = prefetchedInstruction;
      registerR.write(createValue(registerR.read().intValue() + rdelta));
    }

    return currentInstruction;

//    try {
//      if (prefetch) {
//        int rValue = registerR.read().intValue();
//        T nextPC= createValue(0);
//        prefetchedInstruction = fetchInstruction(nextPC);
//        prefetchPC = nextPC.intValue();
//        rdelta = registerR.read().intValue() - rValue;
//        registerR.write(createValue(rValue));
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
//    }
  }

  public Instruction<T> fetchInstruction(T address) {
    memory.disableReadListener();
    int opcodeInt = memory.read(address, 1).intValue();
    Instruction<T> opcodesTable = opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt];
    Instruction<T> baseInstruction2 = getBaseInstruction2(opcodesTable);
    if (clone)
      baseInstruction2 = new InstructionCloner<T, T>(instructionFactory).clone(baseInstruction2);

    memory.enableReadListener();
    Instruction<T> finalBaseInstruction = baseInstruction2;
    fetchListeners.forEach(l -> l.instructionFetchedAt(address, finalBaseInstruction));
    return baseInstruction2;
  }

  public static <T extends WordNumber> Instruction<T> getBaseInstruction(Instruction<T> instruction) {
    while (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
      Memory memory = fetchNextOpcodeInstruction.getState().getMemory();
      boolean lastCanDiable = memory.canDisable();
      memory.canDisable(true);
      memory.disableReadListener();
      instruction = fetchNextOpcodeInstruction.findNextOpcode();
      memory.enableReadListener();
      memory.canDisable(lastCanDiable);
    }
    return instruction;
  }

  public static <T extends WordNumber> Instruction<T> getBaseInstruction2(Instruction<T> instruction) {
    while (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
      fetchNextOpcodeInstruction.update();
      instruction = fetchNextOpcodeInstruction.findNextOpcode();
    }
    return instruction;
  }


  public static <T extends WordNumber> Instruction<T> processToBase(Instruction<T> instruction) {
    while (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
      fetchNextOpcodeInstruction.update();
      instruction = fetchNextOpcodeInstruction.findNextOpcode();
    }
    return instruction;
  }

  @Override
  public void reset() {
    instructionExecutor.reset();
  }

  @Override
  public void addFetchListener(FetchListener fetchListener) {
    fetchListeners.add(fetchListener);
  }

  @Override
  public void setPrefetch(boolean prefetch) {
    this.prefetch = prefetch;
  }

  @Override
  public InstructionExecutor<T> getInstructionExecutor() {
    return instructionExecutor;
  }
}

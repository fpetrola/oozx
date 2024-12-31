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
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.InstructionFetcherForTest;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.fpetrola.z80.registers.RegisterName.B;

public class DefaultInstructionFetcher<T extends WordNumber> implements InstructionFetcher {
  private final InstructionFactory instructionFactory;
  protected State<T> state;
  protected Instruction<T>[] opcodesTables;

  protected T pcValue;
  protected final InstructionExecutor<T> instructionExecutor;
  FileWriter fileWriter;
  //  List<ExecutedInstruction> lastInstructions = new ArrayList<>();
  protected Supplier<TableBasedOpCodeDecoder> tableFactory;
  protected Instruction<T> instruction;
  public Instruction<T> instruction2;
  private boolean noRepeat;
  private boolean clone;
  private List<FetchListener> fetchListeners = new ArrayList<>();
  private int prefetch= -1;
  private Instruction<T> prefetchedInstruction;

//  {
//    try {
//      fileWriter = new FileWriter(Z80B.FILE);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }

  public DefaultInstructionFetcher(State aState, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat1) {
    this(aState, new OpcodeConditions(aState.getFlag(), aState.getRegister(B)), fetchInstructionFactory, instructionExecutor, instructionFactory, noRepeat1, false);
  }

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat, boolean clone) {
    this.state = aState;
    this.instructionExecutor = instructionExecutor;
    this.noRepeat = noRepeat;
    tableFactory = () -> createOpcodesTables(opcodeConditions, fetchInstructionFactory, instructionFactory);
    createOpcodeTables();
    pcValue = state.getPc().read();
    this.instructionFactory = instructionFactory;
    this.clone = clone;
  }

  protected void createOpcodeTables() {
    opcodesTables = tableFactory.get().getOpcodeLookupTable();
  }

  public TableBasedOpCodeDecoder createOpcodesTables(OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionFactory instructionFactory) {
    return new TableBasedOpCodeDecoder<T>(this.state, opcodeConditions, fetchInstructionFactory, instructionFactory);
  }

  @Override
  public void fetchNextInstruction() {
    state.getRegisterR().increment();
    pcValue = state.getPc().read();
    Memory<T> memory = state.getMemory();
    if (prefetch != pcValue.intValue()) {
      instruction2 = fetchInstruction(pcValue);
      prefetchedInstruction= instruction2;
    } else
      instruction2 = prefetchedInstruction;

    try {
      memory.read(WordNumber.createValue(-1), 1);
      Instruction<T> executedInstruction = this.instructionExecutor.execute(instruction2);
      memory.read(WordNumber.createValue(-2), 1);

      // fileWriter.write(x + "\n");

      this.instruction2 = getBaseInstruction(executedInstruction);

      T nextPC = null;
      if (noRepeat && this.instruction2 instanceof RepeatingInstruction repeatingInstruction)
        repeatingInstruction.setNextPC(null);

      if (this.instruction2 instanceof AbstractInstruction jumpInstruction) {
        nextPC = (T) jumpInstruction.getNextPC();
      }

//      String x = String.format("%04X", pcValue.intValue()) + ": " + opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt] + " -> " + nextPC;
//      System.out.println(x);


      if (nextPC == null)
        nextPC = pcValue.plus(instruction2.getLength());

      state.getPc().write(nextPC);

      prefetchedInstruction = fetchInstruction(nextPC);
      prefetch= nextPC.intValue();
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
  }

  public Instruction<T> fetchInstruction(T address) {
    Memory<T> memory = state.getMemory();
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
}

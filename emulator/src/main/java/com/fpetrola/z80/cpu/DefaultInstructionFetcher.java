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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.NullInstructionSpy;

import java.io.FileWriter;
import java.util.*;
import java.util.function.Supplier;

import static com.fpetrola.z80.registers.RegisterName.B;

public class DefaultInstructionFetcher<T extends WordNumber> implements InstructionFetcher {
  protected State<T> state;
  protected Instruction<T> instruction;
  protected Instruction<T>[] opcodesTables;

  protected int opcodeInt;
  protected T pcValue;
  protected final InstructionExecutor<T> instructionExecutor;
  FileWriter fileWriter;
  List<ExecutedInstruction> lastInstructions = new ArrayList<>();
  protected Supplier<TableBasedOpCodeDecoder> tableFactory;
  public Instruction<T> instruction2;

//  {
//    try {
//      fileWriter = new FileWriter(Z80B.FILE);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }

  public DefaultInstructionFetcher(State aState, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, DefaultInstructionFactory instructionFactory) {
    this(aState, new OpcodeConditions(aState.getFlag(), aState.getRegister(B)), fetchInstructionFactory, instructionExecutor, instructionFactory);
  }

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, DefaultInstructionFactory instructionFactory) {
    this.state = aState;
    this.instructionExecutor = instructionExecutor;
    tableFactory = () -> createOpcodesTables(opcodeConditions, fetchInstructionFactory, instructionFactory);
    createOpcodeTables();
    pcValue = state.getPc().read();
  }

  protected void createOpcodeTables() {
    opcodesTables = tableFactory.get().getOpcodeLookupTable();
  }

  public TableBasedOpCodeDecoder createOpcodesTables(OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, DefaultInstructionFactory instructionFactory) {
    return new TableBasedOpCodeDecoder<T>(this.state, opcodeConditions, fetchInstructionFactory, instructionFactory);
  }

  public static DefaultInstructionFetcher getInstructionFetcher(State state, NullInstructionSpy spy, DefaultInstructionFactory instructionFactory) {
    SpyInstructionExecutor instructionExecutor1 = new SpyInstructionExecutor(spy);
    instructionExecutor1.setMemptrUpdater(new MemptrUpdater<>(state.getMemptr(), state.getMemory()));
    return new DefaultInstructionFetcher(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), new FetchNextOpcodeInstructionFactory(spy, state), instructionExecutor1, instructionFactory);
  }

  @Override
  public void fetchNextInstruction() {
    state.getRegisterR().increment();
    pcValue = state.getPc().read();
//    if (pcValue.intValue() == 5853)
//      System.out.println("dagdag");
    Memory<T> memory = state.getMemory();
    memory.disableReadListener();
    opcodeInt = memory.read(pcValue, 1).intValue();
    Instruction<T> instruction = opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt];
    this.instruction = instruction;
    memory.enableReadListener();

    try {
      lastInstructions.add(new ExecutedInstruction(pcValue.intValue(), this.instruction));
      instruction2 = getBaseInstruction2(this.instruction);

      memory.read(WordNumber.createValue(-1), 1);
      Instruction<T> executedInstruction = this.instructionExecutor.execute(instruction2);
      memory.read(WordNumber.createValue(-2), 1);

      // fileWriter.write(x + "\n");

      this.instruction = getBaseInstruction(executedInstruction);

      T nextPC = null;
      if (this.instruction instanceof AbstractInstruction jumpInstruction) {
        nextPC = (T) jumpInstruction.getNextPC();
      }

      String x = pcValue + ": " + instruction + " -> " + nextPC;
//      System.out.println(x);

      if (nextPC == null)
        nextPC = pcValue.plus(getBaseInstruction(instruction).getLength());

      state.getPc().write(nextPC);
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
  }

  public static <T extends WordNumber> Instruction<T> getBaseInstruction(Instruction<T> instruction) {
    while (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
      Memory memory = fetchNextOpcodeInstruction.getState().getMemory();
      memory.canDisable(true);
      memory.disableReadListener();
      instruction = fetchNextOpcodeInstruction.findNextOpcode();
      memory.enableReadListener();
      memory.canDisable(false);
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
}

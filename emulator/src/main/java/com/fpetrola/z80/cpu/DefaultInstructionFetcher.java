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
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.*;
import java.util.function.Supplier;

import static com.fpetrola.z80.registers.RegisterName.B;

public class DefaultInstructionFetcher<T extends WordNumber> implements InstructionFetcher {
  private final InstructionFactory instructionFactory;
  protected State<T> state;
  protected Instruction<T>[] opcodesTables;

  protected int opcodeInt;
  protected T pcValue;
  protected final InstructionExecutor<T> instructionExecutor;
  List<ExecutedInstruction> lastInstructions = new ArrayList<>();
  protected Supplier<TableBasedOpCodeDecoder> tableFactory;
  protected Instruction<T> lastExecutedInstruction;
  private boolean noRepeat;
  private boolean clone;
  private final Memory<T> memoryForOpcode;
  private Register<T> pc;
  private Register<T> registerR;
  private Memory<T> memory;

  public DefaultInstructionFetcher(State aState, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat1) {
    this(aState, new OpcodeConditions(aState.getFlag(), aState.getRegister(B)), fetchInstructionFactory, instructionExecutor, instructionFactory, noRepeat1, false);
  }

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor<T> instructionExecutor, InstructionFactory instructionFactory, boolean noRepeat, boolean clone) {
    this.state = aState;
    this.instructionExecutor = instructionExecutor;
    this.noRepeat = noRepeat;
    memoryForOpcode = new MemoryForOpcodes(this.state.getMemory());
    tableFactory = () -> createOpcodesTables(opcodeConditions, fetchInstructionFactory, instructionFactory);
    createOpcodeTables();
    pcValue = state.getPc().read();
    this.instructionFactory = instructionFactory;
    this.clone = clone;
    memory = state.getMemory();
    registerR = state.getRegisterR();
    pc = state.getPc();
  }

  protected void createOpcodeTables() {
    opcodesTables = tableFactory.get().getOpcodeLookupTable();
  }

  public TableBasedOpCodeDecoder createOpcodesTables(OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionFactory instructionFactory) {
    return new TableBasedOpCodeDecoder<T>(this.state, opcodeConditions, fetchInstructionFactory, instructionFactory, memoryForOpcode);
  }

  @Override
  public void fetchNextInstruction() {
    registerR.increment();
    pcValue = pc.read();
    memory.disableReadListener();
    opcodeInt = memory.read(pcValue, 1).intValue();
    memoryForOpcode.reset();
    Instruction<T> baseInstruction = processToBase(opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt]);

    if (clone)
      baseInstruction = new InstructionCloner<T, T>(instructionFactory).clone(baseInstruction);

    lastExecutedInstruction = baseInstruction;
    memory.enableReadListener();

    try {
//      lastInstructions.add(new ExecutedInstruction(pcValue.intValue(), this.instruction));

      memory.read(WordNumber.createValue(-1), 1);
      Instruction<T> executedInstruction = this.instructionExecutor.execute(getLastExecutedInstruction());
      memory.read(WordNumber.createValue(-2), 1);

      lastExecutedInstruction = executedInstruction;

      T nextPC = null;
      if (noRepeat && lastExecutedInstruction instanceof RepeatingInstruction repeatingInstruction)
        repeatingInstruction.setNextPC(null);

      if (lastExecutedInstruction instanceof AbstractInstruction jumpInstruction) {
        nextPC = (T) jumpInstruction.getNextPC();
      }

//      String x = String.format("%04X", pcValue.intValue()) + ": " + opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt] + " -> " + nextPC;
//      System.out.println(x);


      if (nextPC == null)
        nextPC = pcValue.plus(lastExecutedInstruction.getLength());

      state.getPc().write(nextPC);
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
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

  public Instruction<T> getLastExecutedInstruction() {
    return lastExecutedInstruction;
  }

  public void setLastExecutedInstruction(Instruction<T> instruction) {
    lastExecutedInstruction = instruction;
  }
}

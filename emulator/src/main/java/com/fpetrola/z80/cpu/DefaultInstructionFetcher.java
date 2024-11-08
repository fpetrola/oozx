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

import com.fpetrola.z80.instructions.base.AbstractInstruction;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.NullInstructionSpy;

import java.io.FileWriter;
import java.util.*;

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
    opcodesTables = new TableBasedOpCodeDecoder<T>(this.state, opcodeConditions, fetchInstructionFactory, instructionFactory).getOpcodeLookupTable();
    pcValue = state.getPc().read();
  }

  public static DefaultInstructionFetcher getInstructionFetcher(State state, NullInstructionSpy spy, DefaultInstructionFactory instructionFactory) {
    return new DefaultInstructionFetcher(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), new FetchNextOpcodeInstructionFactory(spy, state), new SpyInstructionExecutor(spy), instructionFactory);
  }

  @Override
  public void fetchNextInstruction() {
    state.getRegisterR().increment();
    pcValue = state.getPc().read();
//    if (pcValue.intValue() == 5853)
//      System.out.println("dagdag");
    Memory<T> memory = state.getMemory();
    memory.disableReadListener();
    opcodeInt = memory.read(pcValue).intValue();
    Instruction<T> instruction = opcodesTables[this.state.isHalted() ? 0x76 : opcodeInt];
    this.instruction = instruction;
    memory.enableReadListener();

    try {
      lastInstructions.add(new ExecutedInstruction(pcValue.intValue(), this.instruction));
      Instruction<T> executedInstruction = this.instructionExecutor.execute(this.instruction);

      // fileWriter.write(x + "\n");

      this.instruction = getBaseInstruction(executedInstruction);

      T nextPC = null;
      if (this.instruction instanceof AbstractInstruction jumpInstruction) {
        nextPC = (T) jumpInstruction.getNextPC();
      }

      String x = String.format("%04X", pcValue.intValue()) + ": " + instruction + " -> " + nextPC;
//      System.out.println(x);

      if (nextPC == null)
        nextPC = pcValue.plus(getBaseInstruction(instruction).getLength());

      state.getPc().write(nextPC);
    } catch (Exception e) {
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
  }

  public static <T extends WordNumber> Instruction<T> getBaseInstruction(Instruction<T> instruction) {
    while (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
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

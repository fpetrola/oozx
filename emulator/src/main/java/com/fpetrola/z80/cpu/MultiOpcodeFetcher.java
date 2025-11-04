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
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.Register;

import java.util.function.Supplier;

public class MultiOpcodeFetcher<T extends WordNumber> {
  public final InstructionFactory<T> instructionFactory;
  private final State<T> state;
  private final Register<T> registerR;
  public Instruction<T>[] opcodesTables;
  public Supplier<TableBasedOpCodeDecoder<T>> tableFactory;
  public boolean clone;
  private final MemoryForOpcodes<T> memoryForOpcode;
  private final Memory<T> memory;

  public MultiOpcodeFetcher(InstructionFactory<T> instructionFactory, State<T> state, OpcodeConditions opcodeConditions, boolean clone) {
    this.instructionFactory = instructionFactory;
    this.state = state;
    this.clone = clone;
    memoryForOpcode = new MemoryForOpcodes<T>(this.state.getMemory(), this.state);
    tableFactory = () -> createOpcodesTables(opcodeConditions, instructionFactory.getFetchNextOpcodeInstructionFactory(), instructionFactory);
    createOpcodeTables();
    memory = state.getMemory();
    this.registerR = (DefaultRegisterBankFactory.RRegister<T>) state.getRegisterR();
  }

  public void createOpcodeTables() {
    opcodesTables = tableFactory.get().getOpcodeLookupTable();
  }

  public TableBasedOpCodeDecoder<T> createOpcodesTables(OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory<T> fetchInstructionFactory, InstructionFactory<T> instructionFactory) {
    return new TableBasedOpCodeDecoder<T>(state, opcodeConditions, fetchInstructionFactory, instructionFactory, memoryForOpcode);
  }

  public void setClone(boolean clone) {
    this.clone = clone;
  }

  public Instruction<T> fetchInstruction(T address) {
//    T rValue = registerR.read();
    memoryForOpcode.reset();

    Instruction<T> fetchedInstruction = opcodesTables[memory.read(address, 1).value];
    while (fetchedInstruction instanceof DefaultFetchNextOpcodeInstruction<T> fetchNextOpcodeInstruction) {
      fetchNextOpcodeInstruction.update();
      fetchedInstruction = fetchNextOpcodeInstruction.findNextOpcode2();
    }
//    T rdelta = registerR.read().minus(rValue);
//    ((AbstractInstruction<?>) fetchedInstruction).setRDelta(rdelta.intValue());

    if (clone) {
      fetchedInstruction = new InstructionCloner<T, T>(instructionFactory).clone(fetchedInstruction);
    }

    return fetchedInstruction;
  }

  public void reset() {
    createOpcodeTables();
  }

}
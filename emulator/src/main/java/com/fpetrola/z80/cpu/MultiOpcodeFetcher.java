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

import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Supplier;

public class MultiOpcodeFetcher<T extends WordNumber> {
  public final InstructionFactory<T> instructionFactory;
  private final State<T> state;
  public Instruction<T>[] opcodesTables;
  public Supplier<TableBasedOpCodeDecoder<T>> tableFactory;
  public boolean clone;
  private final Memory<T> memoryForOpcode;
  private final Memory<T> memory;

  public MultiOpcodeFetcher(InstructionFactory<T> instructionFactory, State<T> state, OpcodeConditions opcodeConditions, boolean clone) {
    this.instructionFactory = instructionFactory;
    this.state = state;
    this.clone = clone;
    memoryForOpcode = new MemoryForOpcodes<T>(this.state.getMemory());
    tableFactory = () -> createOpcodesTables(opcodeConditions, instructionFactory.getFetchNextOpcodeInstructionFactory(), instructionFactory);
    createOpcodeTables();
    memory = state.getMemory();
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
    memoryForOpcode.reset();
    Instruction<T> opcodesTable = opcodesTables[state.isHalted() ? 0x76 : memory.read(address, 1).intValue()];
    while (opcodesTable instanceof DefaultFetchNextOpcodeInstruction<T> fetchNextOpcodeInstruction) {
      fetchNextOpcodeInstruction.update();
      opcodesTable = fetchNextOpcodeInstruction.findNextOpcode2();
    }
    return opcodesTable;
  }

  public void reset() {
    createOpcodeTables();
  }

}
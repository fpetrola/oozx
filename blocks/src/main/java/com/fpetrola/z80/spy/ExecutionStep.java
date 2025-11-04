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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;

import java.util.ArrayList;
import java.util.List;

public class ExecutionStep {
  public List<WriteOpcodeReference> writeReferences = new ArrayList<>();
  public List<ReadOpcodeReference> readReferences = new ArrayList<>();
  public List<WriteMemoryReference> writeMemoryReferences = new ArrayList<>();
  public List<ReadMemoryReference> readMemoryReferences = new ArrayList<>();
  transient public List<Object> accessReferences = new ArrayList<>();
  private transient Instruction instruction;
  public String description;
  public int pcValue;
  final transient private Memory memory;
  public int i;

  public ExecutionStep(Memory memory) {
    this.memory = memory;
  }

  public WriteOpcodeReference addWriteReference(String opcodeReference, int value, boolean isIncrement, boolean indirectReference) {
    WriteOpcodeReference e = new WriteOpcodeReference(opcodeReference, value, isIncrement, indirectReference);
    writeReferences.add(e);
    addAccessReference(e);
    return e;
  }

  private void addAccessReference(Undoable e) {
    accessReferences.add(e);
  }

  public ReadOpcodeReference addReadReference(String opcodeReference, int value, boolean indirectReference) {
    ReadOpcodeReference e = new ReadOpcodeReference(opcodeReference, value, indirectReference);
    readReferences.add(e);
    addAccessReference(e);
    return e;
  }

  protected void clear() {
    writeReferences.clear();
    readReferences.clear();
    writeMemoryReferences.clear();
    readMemoryReferences.clear();
  }

  public WriteMemoryReference addWriteMemoryReference(int address, int value, boolean indirectReference) {
    WriteMemoryReference e = new WriteMemoryReference(address, value, memory, indirectReference);
    writeMemoryReferences.add(e);
    addAccessReference(e);
    return e;
  }

  public ReadMemoryReference addReadMemoryReference(int address, int value, boolean indirectReference) {
    ReadMemoryReference e = new ReadMemoryReference(address, value, memory, indirectReference);
    readMemoryReferences.add(e);
    addAccessReference(e);
    return e;
  }

  public void undo() {
//    accessReferences.forEach(ar -> ar.undo());
  }

  public void setIndex(int i) {
    this.i = i;
  }

  public Instruction getInstruction() {
    return instruction;
  }

  public void setInstruction(Instruction instruction) {
    this.instruction = instruction;
  }
}
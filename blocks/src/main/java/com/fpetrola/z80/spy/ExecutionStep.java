/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.spy;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.List;

public class ExecutionStep<T extends WordNumber> {
  public List<WriteOpcodeReference> writeReferences = new ArrayList<>();
  public List<ReadOpcodeReference> readReferences = new ArrayList<>();
  public List<WriteMemoryReference<T>> writeMemoryReferences = new ArrayList<>();
  public List<ReadMemoryReference<T>> readMemoryReferences = new ArrayList<>();
  transient public List<Object> accessReferences = new ArrayList<>();
  private transient Instruction<T> instruction;
  public String description;
  public int pcValue;
  transient private Memory memory;
  public int i;

  public ExecutionStep(Memory memory) {
    this.memory = memory;
  }

  public WriteOpcodeReference addWriteReference(String opcodeReference, T value, boolean isIncrement, boolean indirectReference) {
    WriteOpcodeReference e = new WriteOpcodeReference(opcodeReference, value, isIncrement, indirectReference);
    writeReferences.add(e);
    addAccessReference(e);
    return e;
  }

  private void addAccessReference(Undoable e) {
    accessReferences.add(e);
  }

  public ReadOpcodeReference addReadReference(String opcodeReference, T value, boolean indirectReference) {
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

  public WriteMemoryReference addWriteMemoryReference(T address, T value, boolean indirectReference) {
    WriteMemoryReference e = new WriteMemoryReference(address, value, memory, indirectReference);
    writeMemoryReferences.add(e);
    addAccessReference(e);
    return e;
  }

  public ReadMemoryReference<T> addReadMemoryReference(T address, T value, boolean indirectReference) {
    ReadMemoryReference<T> e = new ReadMemoryReference<T>(address, value, memory, indirectReference);
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

  public Instruction<T> getInstruction() {
    return instruction;
  }

  public void setInstruction(Instruction<T> instruction) {
    this.instruction = instruction;
  }
}
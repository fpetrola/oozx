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

package com.fpetrola.z80.opcodes.decoder;

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.InstructionSpy;

import static com.fpetrola.z80.registers.RegisterName.PC;

public class DefaultFetchNextOpcodeInstruction<T extends WordNumber> extends AbstractInstruction<T> implements FetchNextOpcodeInstruction<T> {
  private boolean incrementR;
  private final Register<T> pc;
  private final Instruction[] table;
  private final String name;
  public final Memory<T> memoryForOpcodes;
  private final int incPc;
  private final InstructionSpy spy;
  private final Register registerR;

  public DefaultFetchNextOpcodeInstruction(State state, Instruction[] table, int incPc, String name, InstructionSpy spy, Memory<T> memoryForOpcodes) {
    this.table = table;
    this.name = name;
    this.memoryForOpcodes = memoryForOpcodes;
    for (int i = 0; i < table.length; i++) {
      if (table[i] != null)
        ((AbstractInstruction) table[i]).setLength(table[i].getLength() + 1);
    }
    this.incPc = incPc;
    this.spy = spy;
    this.registerR = state.getRegister(RegisterName.R);
    this.pc = state.getRegister(PC);
    incrementR = name.length() == 2;
  }

  public int execute() {
    findNextOpcode().execute();
    return 4;
  }

  public void update() {
    if (incrementR)
      registerR.increment();
  }

  public Instruction<T> findNextOpcode() {
    return (Instruction<T>) table[memoryForOpcodes.read(pc.read().plus(incPc - 1 + length), incPc).intValue()];
  }

  public String toString() {
    return "STRING:DefaultFetchNextOpcodeInstruction";//findNextOpcode().toString();
  }

  public Instruction[] getTable() {
    return table;
  }
}
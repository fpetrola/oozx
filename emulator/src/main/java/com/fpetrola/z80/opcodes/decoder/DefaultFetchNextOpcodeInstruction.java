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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.InstructionSpy;

import static com.fpetrola.z80.registers.RegisterName.PC;

public class DefaultFetchNextOpcodeInstruction<T extends WordNumber> extends AbstractInstruction<T> implements FetchNextOpcodeInstruction<T> {

  @Override
  public State<T> getState() {
    return state;
  }

  private final State<T> state;
  private final Register<T> pc;
  private Instruction[] table;
  private int incPc;
  private String name;
  private InstructionSpy spy;
  private Register registerR;

  public DefaultFetchNextOpcodeInstruction(State state, Instruction[] table, int incPc, String name, InstructionSpy spy) {
    this.state = state;
    this.table = table;
    for (int i = 0; i < table.length; i++) {
      if (table[i] != null)
        ((AbstractInstruction) table[i]).setLength(table[i].getLength() + 1);
    }
    this.incPc = incPc;
    this.name = name;
    this.spy = spy;
    this.registerR = state.getRegister(RegisterName.R);
    this.pc = state.getRegister(PC);
  }

  public int execute() {
    spy.pause();
    update();
    Instruction<T> instruction = findNextOpcode();
    spy.doContinue();
    instruction.execute();
    return 4;
  }

  public void update() {
    if (name.length() == 2)
      registerR.increment();
  }

  public Instruction findNextOpcode() {
    spy.pause();
    Memory<T> memory = state.getMemory();
    //memory.disableReadListener();
    int opcodeInt = memory.read(pc.read().plus(incPc - 1 + length), incPc).intValue();
    Instruction instruction = table[opcodeInt];
    spy.flipOpcode(instruction, opcodeInt);
    // memory.enableReadListener();
    spy.doContinue();
    return instruction;
  }

  public String toString() {
    return "STRING:DefaultFetchNextOpcodeInstruction";//findNextOpcode().toString();
  }

  public int getIncPc() {
    return incPc;
  }

  public Instruction[] getTable() {
    return table;
  }
}
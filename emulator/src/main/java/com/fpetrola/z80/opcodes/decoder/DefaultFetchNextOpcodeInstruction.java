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

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.Memory8BitReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.function.IntConsumer;

import static com.fpetrola.z80.registers.RegisterName.PC;

public class DefaultFetchNextOpcodeInstruction extends AbstractInstruction implements FetchNextOpcodeInstruction {
  private boolean incrementR;
  private final Register pc;
  private final Instruction[] table;
  private final String name;
  public final Memory memoryForOpcodes;
  private final int incPc;
  private final Register registerR;
  private IntConsumer inc2Consumer;
  private IntConsumer inc1Consumer;

  public DefaultFetchNextOpcodeInstruction(State state, Instruction[] table, int incPc, String name, Memory memoryForOpcodes) {
    this.table = table;
    this.name = name;
    this.memoryForOpcodes = memoryForOpcodes;
    for (Instruction instruction : table) {
      if (instruction != null)
        ((AbstractInstruction) instruction).setLength(instruction.getLength() + 1);
    }
    this.incPc = incPc;
    this.registerR = state.getRegister(RegisterName.R);
    this.pc = state.getRegister(PC);
    incrementR = name.length() == 2;

    IntConsumer nullConsumer = (a) -> {
    };
    if (incPc == 2) {
      inc2Consumer = plus1 -> memoryForOpcodes.read((plus1 - 1) & 0xFFFF, 0);
    } else
      inc2Consumer = nullConsumer;

    if (incPc == 1)
      inc1Consumer = plus1 -> memoryForOpcodes.read((plus1 + 1) & 0xFFFF, 0);
    else
      inc1Consumer = nullConsumer;
  }

  public int execute() {
    findNextOpcode().execute();
    return 4;
  }

  public void update() {
    if (incrementR)
      registerR.increment();
  }

  public Instruction findNextOpcode() {
    return table[memoryForOpcodes.read((pc.read() + incPc - 1 + length) & 0xFFFF, incPc)];
  }

  public Instruction findNextOpcode2() {
    int plus = (pc.read() + incPc - 1 + length) & 0xFFFF;
    inc2Consumer.accept(plus);
    Instruction instruction = table[memoryForOpcodes.read(plus, incPc)];
    if (instruction instanceof Ld ld && ld.getSource() instanceof Memory8BitReference)
      inc1Consumer.accept(plus);

    return instruction;
  }

  public String toString() {
    return "STRING:DefaultFetchNextOpcodeInstruction";//findNextOpcode().toString();
  }

  public Instruction[] getTable() {
    return table;
  }
}
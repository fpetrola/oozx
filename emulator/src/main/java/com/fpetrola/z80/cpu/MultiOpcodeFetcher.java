/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.Z80UnRolled;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.ToStringInstructionVisitor;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.opcodes.decoder.table.TableBasedOpCodeDecoder;
import com.fpetrola.z80.opcodes.references.Memory8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.registers.Register;

import java.util.HashSet;
import java.util.function.Supplier;

public class MultiOpcodeFetcher {
  public final InstructionFactory instructionFactory;
  private final State state;
  public final FetchedInstructionWrapper[] opcodesTables = new FetchedInstructionWrapper[0x100];
  public Supplier<TableBasedOpCodeDecoder> tableFactory;
  private final OpcodeConditions opcodeConditions;
  public boolean clone;
  private final MemoryForOpcodes memoryForOpcode;
  private final Memory memory;
  private final Register pc;
  private Z80UnRolled registerBank;
  private HashSet<Instruction> instructions = new HashSet<>();

  public MultiOpcodeFetcher(InstructionFactory instructionFactory, State state, OpcodeConditions opcodeConditions, boolean clone) {
    this.instructionFactory = instructionFactory;
    this.state = state;
    this.pc = state.getPc();
    this.opcodeConditions = opcodeConditions;
    this.clone = clone;
    memoryForOpcode = new MemoryForOpcodes(this.state.getMemory(), this.state);
    tableFactory = () -> getOpcodesTables();
    createOpcodeTables();
    memory = state.getMemory();

    if ((this.state.getRegisters() instanceof Z80UnRolled)) {
      registerBank = (Z80UnRolled) this.state.getRegisters();
      registerBank.setMemory(state.getMemory());
    }
  }

  public TableBasedOpCodeDecoder getOpcodesTables() {
    return createOpcodesTables(opcodeConditions, instructionFactory.getFetchNextOpcodeInstructionFactory(), instructionFactory);
  }

  public void createOpcodeTables() {
    wrapInstructions(tableFactory.get().getOpcodeLookupTable(), 0, pc, opcodesTables, -1, false, memory);
  }

  public static void wrapInstructions(Instruction[] instructions, int increment, Register pc, FetchedInstructionWrapper[] wrappers, int incPc, boolean incrementR, Memory memory) {
    for (int i = 0; i < instructions.length; i++) {
      Instruction instruction = instructions[i];
      if (instruction != null) {
        if (instruction instanceof Ld ld && ld.getSource() instanceof Memory8BitReference && incPc == 1) {
          wrappers[i] = new LdSpecialWrapper(instruction, increment, pc, memory);
        } else if (instruction instanceof DefaultFetchNextOpcodeInstruction fetchNextOpcodeInstruction) {
          wrappers[i] = new FetchNextOpcodeInstructionWrapper(fetchNextOpcodeInstruction);
        } else
          wrappers[i] = new FetchedInstructionWrapper(instruction);
      }
    }
  }

  public TableBasedOpCodeDecoder createOpcodesTables(OpcodeConditions
                                                         opcodeConditions, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionFactory instructionFactory) {
    return new TableBasedOpCodeDecoder(state, opcodeConditions, fetchInstructionFactory, instructionFactory, memoryForOpcode);
  }

  public void setClone(boolean clone) {
    this.clone = clone;
  }

  public Instruction fetchInstruction(int address) {
    memoryForOpcode.reset();

    FetchedInstructionWrapper fetchedInstructionWrapper = opcodesTables[memory.read(address, 1)];

    int execute = -1;
    int read = pc.read();

    pc.write(read + 1);
    if (registerBank != null)
      execute = registerBank.execute(memory.read(address, 1));

    pc.write(read);
    if (execute == 0) {
      pc.write((read + fetchedInstructionWrapper.getInstruction().getLength()) & 0xFFFF);
      return null;
    } else {
      AbstractInstruction instruction = (AbstractInstruction) fetchedInstructionWrapper.getInstruction();
//      if (!instructions.contains(instruction)) {
//        System.out.println(new ToStringInstructionVisitor().createToString(instruction));
//        instructions.add(instruction);
//      }
      instruction.execute();
      int nextPC = instruction.getNextPC();
      if (nextPC == -1) {
        nextPC = (pc.read() + instruction.getLength()) & 0xFFFF;
      }
      pc.write(nextPC);
      return instruction;
    }
  }

  public void reset() {
    createOpcodeTables();
  }

}
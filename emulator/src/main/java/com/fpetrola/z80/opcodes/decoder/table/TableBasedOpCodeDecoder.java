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

package com.fpetrola.z80.opcodes.decoder.table;

import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.OpCodeDecoder;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeTargets;
import com.fpetrola.z80.registers.RegisterName;

import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TableBasedOpCodeDecoder implements OpCodeDecoder {
  Instruction[] opcodes = new Instruction[0x100];
  InstructionFactory instructionFactory;

  public TableBasedOpCodeDecoder(State state, OpcodeConditions oc, FetchNextOpcodeInstructionFactory fif, InstructionFactory instructionFactory, Memory memoryForOpcodes) {
//    memoryForOpcodes= state.getMemory();
    this.instructionFactory = instructionFactory;
    OpcodeTargets opcodeTargets = new OpcodeTargets(state, memoryForOpcodes);
    OpcodeReference a = opcodeTargets.iRR(HL);
    Instruction edOpcode = fif.createFetchInstruction(new EDPrefixTableOpCodeGenerator(state, a, oc, this.instructionFactory, memoryForOpcodes).getOpcodesTable(), "ED", 1, memoryForOpcodes);
    Instruction cbOpcode = fif.createFetchInstruction(new CBPrefixTableOpCodeGenerator(state, a, oc, this.instructionFactory, memoryForOpcodes).getOpcodesTable(), "CB", 1, memoryForOpcodes);
    Instruction ddOpcode = fif.createFetchInstruction(fillDDFD(state, IX, IXH, IXL, opcodeTargets.iRRn(IX, false, 2), oc, fif, memoryForOpcodes), "DD", 1, memoryForOpcodes);
    Instruction fdOpcode = fif.createFetchInstruction(fillDDFD(state, IY, IYH, IYL, opcodeTargets.iRRn(IY, false, 2), oc, fif, memoryForOpcodes), "FD", 1, memoryForOpcodes);
    UnprefixedTableOpCodeGenerator unprefixedTableOpCodeGenerator = new UnprefixedTableOpCodeGenerator(1, state, cbOpcode, ddOpcode, edOpcode, fdOpcode, HL, H, L, a, oc, this.instructionFactory, memoryForOpcodes);
    opcodes = unprefixedTableOpCodeGenerator.getOpcodesTable();
  }

  private Instruction[] fillDDFD(State s, RegisterName registerName, final RegisterName highRegisterName, final RegisterName lowRegisterName, OpcodeReference a, OpcodeConditions opcodeConditions, FetchNextOpcodeInstructionFactory fetchNextOpcodeInstructionFactory, Memory memoryForOpcodes) {
    Instruction cbOpcode = fetchNextOpcodeInstructionFactory.createFetchInstruction(new DDCBFDCBPrefixTableOpCodeGenerator(s, registerName, highRegisterName, lowRegisterName, a, opcodeConditions, instructionFactory, memoryForOpcodes).getOpcodesTable(), "DDFDCB", 2, memoryForOpcodes);
    UnprefixedTableOpCodeGenerator ddTableOpCodeGenerator = new IndexerRegisterTableOpCodeGenerator(s, cbOpcode, this.instructionFactory.Nop(), this.instructionFactory.Nop(), this.instructionFactory.Nop(), registerName, highRegisterName, lowRegisterName, a, lowRegisterName, highRegisterName, registerName, opcodeConditions, instructionFactory, memoryForOpcodes);
    return ddTableOpCodeGenerator.getOpcodesTable();
  }

  public Instruction[] getOpcodeLookupTable() {
    return opcodes;
  }

}

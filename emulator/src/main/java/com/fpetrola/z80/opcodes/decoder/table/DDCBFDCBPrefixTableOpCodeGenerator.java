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

import com.fpetrola.z80.instructions.base.AbstractInstruction;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.DefaultInstructionFactory;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.RegisterName;

public class DDCBFDCBPrefixTableOpCodeGenerator<T> extends TableOpCodeGenerator<T> {

  private RegisterName ixy;

  public DDCBFDCBPrefixTableOpCodeGenerator(State state, RegisterName ixy, RegisterName ixyh, RegisterName ixyl, OpcodeReference a, OpcodeConditions opcodeConditions, DefaultInstructionFactory instructionFactory) {
    super(state, ixy, ixyh, ixyl, a, opcodeConditions, instructionFactory);
    this.ixy = ixy;
  }

  protected Instruction<T> getOpcode() {
    Instruction result = null;
    switch (x) {
      case 0:
        result = z != 6 ? i.LdOperation(r[z], rot.get(y).create(iRRn(ixy, true, 2), 1)) : rot.get(y).create(iRRn(ixy, true, 2), 1);
        break;
      case 1:
        result = i.BIT(iRRn(ixy, true, 2), y);
        break;
      case 2:
        result = z != 6 ? i.LdOperation(r[z], i.RES(iRRn(ixy, true, 2), y)) : i.RES(iRRn(ixy, true, 2), y);
        break;
      case 3:
        result = z != 6 ? i.LdOperation(r[z], i.SET(iRRn(ixy, true, 2), y)) : i.SET(iRRn(ixy, true, 2), y);
    }
    ((AbstractInstruction) result).setLength(result.getLength() + 1);
    return result;
  }
}

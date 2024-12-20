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
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.RegisterName;

public class IndexerRegisterTableOpCodeGenerator<T> extends UnprefixedTableOpCodeGenerator<T> {
  private final RegisterName lowRegisterName;
  private final RegisterName highRegisterName;
  private final RegisterName registerName;

  public IndexerRegisterTableOpCodeGenerator(State state, Instruction<T> cbOpcode, Instruction<T> ddOpcode, Instruction<T> edOpcode, Instruction<T> fdOpcode, RegisterName main16BitRegister, RegisterName mainHigh8BitRegister, RegisterName mainLow8BitRegister, OpcodeReference main16BitRegisterReference, RegisterName lowRegisterName, RegisterName highRegisterName, RegisterName registerName, OpcodeConditions opc1, InstructionFactory instructionFactory) {
    super(2, state, cbOpcode, ddOpcode, edOpcode, fdOpcode, main16BitRegister, mainHigh8BitRegister, mainLow8BitRegister, main16BitRegisterReference, opc1, instructionFactory);
    this.lowRegisterName = lowRegisterName;
    this.highRegisterName = highRegisterName;
    this.registerName = registerName;
  }

  protected Ld createLd() {
    OpcodeReference target = r[y];
    OpcodeReference source = r[z];

    if (isHL(source) || isHL(target)) {
      source = replaceLowHigh(source, lowRegisterName, highRegisterName);
      target = replaceLowHigh(target, lowRegisterName, highRegisterName);
    }
    return i.Ld(target, source);
  }

  private boolean isHL(ImmutableOpcodeReference source) {
    return source instanceof MemoryPlusRegister8BitReference;
  }

  protected Ld createLd1() {
    OpcodeReference target = r[y];
    if (isHL(target))
      return i.Ld(iRRn(registerName, false, 2), n(3));
    else
      return i.Ld(target, n(2));
  }
}
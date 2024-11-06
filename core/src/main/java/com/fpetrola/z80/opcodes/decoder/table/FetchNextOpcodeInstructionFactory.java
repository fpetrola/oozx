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

import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.spy.InstructionSpy;

public class FetchNextOpcodeInstructionFactory<T> {
  private InstructionSpy spy;
  private State state;

  public FetchNextOpcodeInstructionFactory(InstructionSpy spy, State state) {
    this.spy = spy;
    this.state = state;
  }

  public DefaultFetchNextOpcodeInstruction createFetchInstruction(Instruction<T>[] opcodesTable, String name, int incPc) {
    return new DefaultFetchNextOpcodeInstruction(this.state, opcodesTable, incPc, name, this.spy);
  }
}

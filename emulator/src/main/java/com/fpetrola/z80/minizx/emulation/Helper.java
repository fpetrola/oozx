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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;

import static com.fpetrola.z80.registers.RegisterName.B;

public class Helper {
  public static <T extends WordNumber> OOZ80<T> createOOZ80(IO<T> io) {
    var state = new State<T>(io, new MockedMemory<T>(true));
    return new OOZ80<T>(state, getInstructionFetcher(state, new MemptrUpdateInstructionSpy<T>(state), new DefaultInstructionFactory<T>(state), false));
  }

  public static DefaultInstructionFetcher getInstructionFetcher(State state, InstructionSpy spy, DefaultInstructionFactory instructionFactory, boolean clone) {
    return getInstructionFetcher2(state, spy, instructionFactory, clone, new SpyInstructionExecutor(spy, state));
  }

  public static DefaultInstructionFetcher getInstructionFetcher2(State state, InstructionSpy spy, DefaultInstructionFactory instructionFactory, boolean clone, SpyInstructionExecutor instructionExecutor2) {
    SpyInstructionExecutor instructionExecutor1 = instructionExecutor2;
    return new DefaultInstructionFetcher(state, new OpcodeConditions(state.getFlag(), state.getRegister(B)), new FetchNextOpcodeInstructionFactory(spy, state), instructionExecutor1, instructionFactory, false, clone);
  }
}

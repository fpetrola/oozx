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

package com.fpetrola.z80.factory;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;

import static com.fpetrola.z80.registers.RegisterName.B;

public class Z80Factory {
  public static <T extends WordNumber> OOZ80<T> createOOZ80(IO<T> io) {
    var state = new State<T>(io, new MockedMemory<T>(true));
    return createOOZ80(state, getInstructionFetcher(state, new MemptrUpdateInstructionSpy<T>(state), new DefaultInstructionFactory<T>(state), false));
  }

  public static DefaultInstructionFetcher getInstructionFetcher(State state, InstructionSpy spy, DefaultInstructionFactory instructionFactory, boolean clone) {
    DefaultInstructionFetcher instructionFetcher2 = getInstructionFetcher2(state, instructionFactory, clone, new DefaultInstructionExecutor<>(state), false);
    spy.addExecutionListeners(instructionFetcher2.getInstructionExecutor());
    return instructionFetcher2;
  }

  public static DefaultInstructionFetcher getInstructionFetcher2(State state, DefaultInstructionFactory instructionFactory, boolean clone, DefaultInstructionExecutor instructionExecutor2, boolean prefetch) {
    return new DefaultInstructionFetcher(state, instructionExecutor2, instructionFactory, false, clone, prefetch);
  }

  public static <T extends WordNumber> OOZ80<T> createOOZ80(State aState, InstructionFetcher instructionFetcher) {
    return new OOZ80<T>(aState, instructionFetcher);
  }
}

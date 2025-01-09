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
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;

public class Z80Factory {
  public static <T extends WordNumber> OOZ80<T> createOOZ80(IO<T> io) {
    var state = new State<T>(io, new MockedMemory<T>(true));
    InstructionSpy spy = new MemptrUpdateInstructionSpy<T>(state);
    DefaultInstructionFetcher instructionFetcher = getInstructionFetcher2(state, false, false);
    spy.addExecutionListeners(instructionFetcher.getInstructionExecutor());
    return createOOZ80(state, instructionFetcher);
  }

  private static DefaultInstructionFetcher getInstructionFetcher2(State state, boolean clone, boolean prefetch) {
    return new DefaultInstructionFetcher(state, false, clone, prefetch);
  }

  public static <T extends WordNumber> OOZ80<T> createOOZ80(State aState, InstructionFetcher instructionFetcher) {
    return new OOZ80<T>(aState, instructionFetcher);
  }

  public static <T extends WordNumber> OOZ80<T> createOOZ80(State aState) {
    return createOOZ80(aState, getInstructionFetcher2(aState, false, false));
  }

  public static OOZ80 createOOZ80(State state, InstructionExecutor instructionExecutor) {
    return createOOZ80(state, new DefaultInstructionFetcher(state, instructionExecutor, false, false, false));
  }
}

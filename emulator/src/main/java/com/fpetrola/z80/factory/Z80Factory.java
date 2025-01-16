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
    OOZ80<T> ooz80 = createOOZ80(state, instructionFetcher);
    spy.addExecutionListeners(ooz80.getInstructionExecutor());
    return ooz80;
  }

  public static <T extends WordNumber> OOZ80<T> createOOZ80(State aState, InstructionFetcher instructionFetcher) {
    InstructionExecutor<T> instructionExecutor = new DefaultInstructionExecutor<>(aState, false);
    return createOOZ80(aState, instructionFetcher, instructionExecutor);
  }

  public static <T extends WordNumber> OOZ80<T> createOOZ80(State aState, InstructionFetcher instructionFetcher, InstructionExecutor<T> instructionExecutor) {
    return new OOZ80<T>(aState, instructionFetcher, instructionExecutor);
  }

  public static OOZ80 createOOZ80(State state) {
    return createOOZ80(state, new DefaultInstructionFetcher(state, false, false));
  }

  private static DefaultInstructionFetcher getInstructionFetcher2(State state, boolean clone, boolean prefetch) {
    return new DefaultInstructionFetcher(state, clone, prefetch);
  }
}

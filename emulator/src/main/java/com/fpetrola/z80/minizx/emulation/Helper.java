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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;

public class Helper {
  public static  OOZ80 createOOZ80(IO io) {
    var state = new State(io, new MockedMemory(true));
    return new OOZ80(state, getInstructionFetcher(state, new MemptrUpdateInstructionSpy(state), new DefaultInstructionFactory(state)), new DefaultInstructionExecutor(state, false));
  }

  public static DefaultInstructionFetcher getInstructionFetcher(State state, InstructionSpy spy, DefaultInstructionFactory instructionFactory) {
    return new DefaultInstructionFetcher(state, instructionFactory, false, false);
//    return new CachedInstructionFetcher(state, instructionFactory, false);
  }
}

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

package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.cpu.SpyInstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;
import com.fpetrola.z80.transformations.TransformerInstructionExecutor;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TransformationsTestBaseModule<T extends WordNumber> extends BaseModule {
  @Provides
  @Inject
  @Singleton
  private SpyInstructionExecutor getInstructionExecutor(RoutineFinderInstructionSpy routineFinderInstructionSpy1, State state) {
    return new SpyInstructionExecutor(routineFinderInstructionSpy1, state);
  }

  @Provides
  @Inject
  private TransformerInstructionExecutor getTransformerInstructionExecutor(State state1, SpyInstructionExecutor tInstructionExecutor, InstructionTransformer instructionTransformer) {
    return new TransformerInstructionExecutor<T>(state1.getPc(), tInstructionExecutor, true, instructionTransformer);
  }
}

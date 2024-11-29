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

import com.fpetrola.z80.base.IDriverConfigurator;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.SpyInstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class RoutinesModule<T extends WordNumber> extends BaseModule<T> {
  protected void configure() {
    bind(IDriverConfigurator.class).to(RoutinesDriverConfigurator.class);
    bind(InstructionSpy.class).to(RegisterTransformerInstructionSpy.class);
    bind(InstructionExecutor.class).to(SpyInstructionExecutor.class);
  }

//  @Provides
//  @Inject
//  private RoutinesDriverConfigurator getRoutinesDriverConfigurator(RoutineManager routineManager, RegisterTransformerInstructionSpy spy, State state2) {
//    return new RoutinesDriverConfigurator<>(routineManager, spy, state2);
//  }


}
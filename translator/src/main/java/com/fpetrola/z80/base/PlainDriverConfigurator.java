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

package com.fpetrola.z80.base;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.transformations.*;
import com.google.inject.Inject;

public class PlainDriverConfigurator<T extends WordNumber> extends DriverConfigurator<T> {

  @Inject
  public PlainDriverConfigurator(RoutineManager routineManager, RoutineFinderInstructionSpy spy, State state2, DefaultInstructionExecutor instructionExecutor2, SymbolicExecutionAdapter symbolicExecutionAdapter, InstructionTransformer instructionCloner2, TransformerInstructionExecutor transformerInstructionExecutor1, OpcodeConditions opcodeConditions1, RegistersSetter registersSetter1, CPUExecutionContext secondContext2) {
    super(routineManager, spy, state2, instructionExecutor2, symbolicExecutionAdapter, instructionCloner2, transformerInstructionExecutor1, opcodeConditions1, registersSetter1, secondContext2);
  }

  public CPUExecutionContext<T> getSecondContext() {
    InstructionFetcherForTest<T> instructionFetcher = new InstructionFetcherForTest<>(this.state1);
    OOZ80 z80 = Z80Factory.createOOZ80(state1, instructionFetcher);
    return new CPUExecutionContext<>(spy, z80, opcodeConditions);
  }
}

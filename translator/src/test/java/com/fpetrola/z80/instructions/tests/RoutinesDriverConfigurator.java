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

import com.fpetrola.z80.base.CPUExecutionContext;
import com.fpetrola.z80.base.DriverConfigurator;
import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;
import com.google.inject.Inject;

public class RoutinesDriverConfigurator<T extends WordNumber> extends DriverConfigurator<T> {
  @Inject
  public RoutinesDriverConfigurator(RoutineManager routineManager, RoutineFinderInstructionSpy routineFinderInstructionSpy1, State state2, SymbolicExecutionAdapter symbolicExecutionAdapter1, InstructionTransformer instructionCloner2, InstructionExecutor transformerInstructionExecutor, RegistersSetter registersSetter1, CPUExecutionContext aContext) {
    super(routineManager, routineFinderInstructionSpy1, state2, symbolicExecutionAdapter1, instructionCloner2, transformerInstructionExecutor, symbolicExecutionAdapter1.createOpcodeConditions(state2), registersSetter1, aContext);
  }

  public RealCodeBytecodeCreationBase getRealCodeBytecodeCreationBase() {
    OOZ80 z80 = new OOZ80(state1, symbolicExecutionAdapter.createInstructionFetcher(spy, state1, instructionExecutor, opcodeConditions));
    return new RealCodeBytecodeCreationBase<T>(spy, routineManager, symbolicExecutionAdapter, instructionExecutor, z80, this.opcodeConditions, registersSetter);
  }
}

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
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RoutineFinderInstructionSpy;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.google.inject.Inject;

public class RoutinesDriverConfigurator<T extends WordNumber> extends DriverConfigurator<T> {

  private final StackAnalyzer stackAnalyzer;
  private OOZ80 z80;

  @Inject
  public RoutinesDriverConfigurator(RoutineManager routineManager, RoutineFinderInstructionSpy routineFinderInstructionSpy1,
                                    State state2, InstructionExecutor instructionExecutor2, SymbolicExecutionAdapter symbolicExecutionAdapter1,
                                    InstructionTransformer instructionCloner2, InstructionExecutor transformerInstructionExecutor,
                                    RegistersSetter registersSetter1, CPUExecutionContext secondContext2, StackAnalyzer stackAnalyzer) {
    super(routineManager, routineFinderInstructionSpy1, state2, instructionExecutor2,
        symbolicExecutionAdapter1, instructionCloner2, transformerInstructionExecutor,
        symbolicExecutionAdapter1.createOpcodeConditions(state2), registersSetter1, secondContext2);
    this.stackAnalyzer = stackAnalyzer;
  }

  public RealCodeBytecodeCreationBase getRealCodeBytecodeCreationBase() {
    OOZ80 z80 = Z80Factory.createOOZ80(state1, symbolicExecutionAdapter.createInstructionFetcher(state1, opcodeConditions), transformerInstructionExecutor);
    return new RealCodeBytecodeCreationBase<T>(spy, routineManager, instructionExecutor1, symbolicExecutionAdapter, instructionTransformer,
        transformerInstructionExecutor, z80, this.opcodeConditions, registersSetter, stackAnalyzer);
  }

  @Override
  public void reset() {
    super.reset();
    if (z80 != null)
      z80.reset();
  }
}

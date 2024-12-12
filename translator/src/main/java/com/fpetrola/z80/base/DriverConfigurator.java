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
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.transformations.*;
import com.google.inject.Inject;

public class DriverConfigurator<T extends WordNumber> implements IDriverConfigurator<T> {
  protected final RoutineManager routineManager;
  protected final RoutineFinderInstructionSpy<T> spy;
  protected final InstructionExecutor instructionExecutor1;
  protected State<T> state1;
  public SymbolicExecutionAdapter symbolicExecutionAdapter;
  protected InstructionTransformer instructionTransformer;
  protected InstructionExecutor<T> transformerInstructionExecutor;
  protected OpcodeConditions opcodeConditions;
  protected RegistersSetter<T> registersSetter;
  private CPUExecutionContext<T> secondContext;

  @Override
  public RoutineFinderInstructionSpy<T> getRoutineFinderInstructionSpy() {
    return spy;
  }

  @Inject
  public DriverConfigurator(RoutineManager routineManager, RoutineFinderInstructionSpy spy, State state2,
                            InstructionExecutor instructionExecutor2, SymbolicExecutionAdapter symbolicExecutionAdapter,
                            InstructionTransformer instructionCloner2, InstructionExecutor transformerInstructionExecutor1,
                            OpcodeConditions opcodeConditions1, RegistersSetter registersSetter1, CPUExecutionContext secondContext2) {
    this.secondContext= secondContext2;
    this.routineManager = routineManager;
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    symbolicExecutionAdapter.reset();
    this.routineManager.reset();
    this.spy = spy;
    this.spy.reset(state2);
    state1 = state2;
    instructionExecutor1 = instructionExecutor2;
    instructionTransformer = instructionCloner2;
    transformerInstructionExecutor = transformerInstructionExecutor1;
    opcodeConditions = opcodeConditions1;
    this.registersSetter = registersSetter1;
  }

  @Override
  public CPUExecutionContext<T> getSecondContext() {
    return secondContext;
  }

  @Override
  public CPUExecutionContext<T> getFirstContext() {
    return getSecondContext();
  }

  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  public void reset() {
    symbolicExecutionAdapter.reset();
  }
}

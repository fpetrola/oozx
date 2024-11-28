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
  protected final RegisterTransformerInstructionSpy<T> spy;
  protected final SpyInstructionExecutor instructionExecutor1;
  protected State<T> state1;
  protected VirtualRegisterFactory virtualRegisterFactory1;
  public SymbolicExecutionAdapter symbolicExecutionAdapter;
  protected InstructionTransformer instructionTransformer;
  protected TransformerInstructionExecutor<T> transformerInstructionExecutor;
  protected OpcodeConditions opcodeConditions;

  @Override
  public RegisterTransformerInstructionSpy<T> getRegisterTransformerInstructionSpy() {
    return spy;
  }

  @Inject
  public DriverConfigurator(RoutineManager routineManager, RegisterTransformerInstructionSpy spy, State state2, SpyInstructionExecutor instructionExecutor2, VirtualRegisterFactory virtualRegisterFactory2, SymbolicExecutionAdapter symbolicExecutionAdapter, InstructionTransformer instructionCloner2, TransformerInstructionExecutor transformerInstructionExecutor1, OpcodeConditions opcodeConditions1) {
    this.routineManager = routineManager;
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    symbolicExecutionAdapter.reset();
    this.routineManager.reset();
    this.spy = spy;
    this.spy.reset(state2);
    state1 = state2;
    instructionExecutor1 = instructionExecutor2;
    virtualRegisterFactory1 = virtualRegisterFactory2;
    instructionTransformer = instructionCloner2;
    transformerInstructionExecutor = transformerInstructionExecutor1;
    opcodeConditions = opcodeConditions1;
  }

  @Override
  public CPUExecutionContext<T> getSecondContext() {
    TransformerInstructionExecutor<T> transformerInstructionExecutor1 = new TransformerInstructionExecutor(this.state1.getPc(), instructionExecutor1, false, instructionTransformer);
    RandomAccessInstructionFetcher randomAccessInstructionFetcher = (address) -> transformerInstructionExecutor1.clonedInstructions.get(address);
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
    InstructionFetcher instructionFetcher1 = new TransformerInstructionFetcher(this.state1, transformerInstructionExecutor1);
    OOZ80 z80 = new OOZ80(state1, instructionFetcher1);
    return new CPUExecutionContext<T>(spy, z80, opcodeConditions);
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
//    state1.getPc().write(WordNumber.createValue(0));
//    routineManager.reset();
//    spy.reset(state1);
  }
}

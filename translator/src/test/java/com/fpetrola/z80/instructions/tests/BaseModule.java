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
import com.fpetrola.z80.base.IDriverConfigurator;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.bytecode.VirtualRegistersRegistersSetter;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.decoder.table.FetchNextOpcodeInstructionFactory;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.se.VirtualRegisterDataflowService;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import static com.fpetrola.z80.registers.RegisterName.B;

public class BaseModule<T extends WordNumber> extends AbstractModule {
  @Provides
  @Singleton
  protected BlocksManager getBlocksManager() {
    return new BlocksManager(new NullBlockChangesListener(), false);
  }

  @Provides
  @Singleton
  protected RoutineManager getRoutineManager() {
    return new RoutineManager();
  }

  @Provides
  @Inject
  @Singleton
  protected RoutineFinder getRoutineFinder(RoutineManager routineManager, StackAnalyzer stackAnalyzer, InstructionExecutor instructionExecutor, State state) {
    RoutineFinder routineFinder = new RoutineFinder(routineManager, stackAnalyzer, state);
    routineFinder.addExecutionListener(instructionExecutor);
    return routineFinder;
  }

  @Provides
  @Inject
  private Memory getMemory() {
    return new MockedMemory<>(true);
  }

  protected void configure() {
//    bind(new TypeLiteral<IDriverConfigurator<T>>(){}).to(new TypeLiteral<DriverConfigurator<T>>() {});
    bind(IDriverConfigurator.class).to(DriverConfigurator.class);
    bind(InstructionSpy.class).to(RoutineFinderInstructionSpy.class);
//    bind(InstructionExecutor.class).to(SpyInstructionExecutor.class);
  }

  @Provides
  @Inject
  @Singleton
  private State getState(Memory aMemory) {
    return new State(new MockedIO(), aMemory);
  }

  @Provides
  @Inject
  @Singleton
  private StackAnalyzer getStackAnalyzer(State state, InstructionExecutor instructionExecutor) {
    StackAnalyzer stackAnalyzer = new StackAnalyzer(state);
    stackAnalyzer.addExecutionListener(instructionExecutor);
    return stackAnalyzer;
  }

  @Provides
  @Inject
  @Singleton
  private RoutineFinderInstructionSpy getSpy(RoutineManager routineManager, BlocksManager blocksManager, InstructionExecutor instructionExecutor, State state) {
    RoutineFinderInstructionSpy<?> routineFinderInstructionSpy1 = new RoutineFinderInstructionSpy<>(routineManager, blocksManager);
    routineFinderInstructionSpy1.addExecutionListeners(instructionExecutor);
    routineFinderInstructionSpy1.wrapMemory(state.getMemory());
    return routineFinderInstructionSpy1;
  }

  @Provides
  @Inject
  @Singleton
  private InstructionExecutor getInstructionExecutor(State state) {
    return new DefaultInstructionExecutor<>(state, true);
  }

  @Provides
  @Inject
  @Singleton
  private SymbolicExecutionAdapter getExecutionAdapter(State state1, RoutineManager routineManager, RoutineFinderInstructionSpy spy, DataflowService dataflowService1, StackAnalyzer stackAnalyzer, RoutineFinder routineFinder) {
    return new SymbolicExecutionAdapter(state1, routineManager, spy, dataflowService1, stackAnalyzer, routineFinder);
  }

  @Provides
  @Inject
  @Singleton
  protected InstructionFactory getInstructionFactory(State state2, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    return new DefaultInstructionFactory(state2);
  }

  @Provides
  @Inject
  @Singleton
  private InstructionTransformer getInstructionTransformer(State state2, VirtualRegisterFactory virtualRegisterFactory2, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    return new InstructionTransformer(symbolicExecutionAdapter1.createInstructionFactory(state2), virtualRegisterFactory2);
  }

//  @Provides
//  @Inject
//  private TransformerInstructionExecutor getTransformerInstructionExecutor(State state1, SpyInstructionExecutor tInstructionExecutor, InstructionTransformer instructionTransformer) {
//    return new TransformerInstructionExecutor<T>(state1.getPc(), tInstructionExecutor, true, instructionTransformer);
//  }

  @Provides
  @Inject
  protected OpcodeConditions getOpcodeConditions(State state1) {
    return OpcodeConditions.createOpcodeConditions(state1.getFlag(), state1.getRegister(B));
  }

  @Provides
  @Inject
  @Singleton
  protected DataflowService getDataflowService(State state) {
    return new VirtualRegisterDataflowService(state);
  }

  @Provides
  @Inject
  private RegistersSetter getRegistersSetter(State state1, VirtualRegisterFactory virtualRegisterFactory) {
    return new VirtualRegistersRegistersSetter<>(state1, virtualRegisterFactory);
  }

  @Provides
  @Inject
  public CPUExecutionContext getSecondContext(State state1, RoutineManager routineManager, InstructionExecutor instructionExecutor1, InstructionTransformer instructionTransformer, MutableOpcodeConditions opcodeConditions, InstructionSpy spy, SymbolicExecutionAdapter symbolicExecutionAdapter1, FetchNextOpcodeInstructionFactory fetchInstructionFactory, InstructionExecutor instructionExecutor, DefaultInstructionFactory instructionFactory) {
//    TransformerInstructionExecutor<T> transformerInstructionExecutor1 = new TransformerInstructionExecutor(state1.getPc(), instructionExecutor1, false, instructionTransformer);
    RandomAccessInstructionFetcher randomAccessInstructionFetcher = (address) -> instructionExecutor1.getInstructionAt(address);
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
//    InstructionFetcher instructionFetcher1 = new TransformerInstructionFetcher(state1, transformerInstructionExecutor1);
    InstructionFetcher instructionFetcher1 = new InstructionFetcherForTest<>(state1, instructionExecutor);
    OOZ80 z80 = Z80Factory.createOOZ80(state1, instructionFetcher1);
    return new CPUExecutionContext<T>(spy, z80, opcodeConditions);
  }


  @Provides
  @Inject
  @Singleton
  public FetchNextOpcodeInstructionFactory getMutableOpcodeConditions(State state1) {
    return new FetchNextOpcodeInstructionFactory(state1);
  }

  @Provides
  @Inject
  @Singleton
  public MutableOpcodeConditions getMutableOpcodeConditions(State state1, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    return symbolicExecutionAdapter1.createOpcodeConditions(state1);
  }

  @Provides
  @Inject
  @Singleton
  public VirtualRegisterFactory getVirtualRegisterFactory(InstructionExecutor instructionExecutor, RegisterNameBuilder registerNameBuilder, BlocksManager blocksManager) {
    return new VirtualRegisterFactory<>(instructionExecutor, registerNameBuilder, blocksManager);
  }
}

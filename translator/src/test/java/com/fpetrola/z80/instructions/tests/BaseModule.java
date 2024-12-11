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
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.references.MutableOpcodeConditions;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.DataflowService;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.se.VirtualRegisterDataflowService;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.SpyRegisterBankFactory;
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
  private Memory getMemory(RoutineFinderInstructionSpy spy) {
    return spy.wrapMemory(new MockedMemory<>(true));
  }

  protected void configure() {
//    bind(new TypeLiteral<IDriverConfigurator<T>>(){}).to(new TypeLiteral<DriverConfigurator<T>>() {});
    bind(IDriverConfigurator.class).to(DriverConfigurator.class);
    bind(InstructionSpy.class).to(RoutineFinderInstructionSpy.class);
    bind(InstructionExecutor.class).to(SpyInstructionExecutor.class);
  }

  @Provides
  @Inject
  @Singleton
  private State getState(RoutineFinderInstructionSpy spy, Memory aMemory) {
    return new State(new MockedIO(), new SpyRegisterBankFactory<>(spy).createBank(), aMemory);
  }

  @Provides
  @Inject
  @Singleton
  private RoutineFinderInstructionSpy getSpy(RoutineManager routineManager, BlocksManager blocksManager) {
    return new RoutineFinderInstructionSpy<>(routineManager, blocksManager);
  }

  @Provides
  @Inject
  @Singleton
  private SpyInstructionExecutor getInstructionExecutor(RoutineFinderInstructionSpy routineFinderInstructionSpy1, State state) {
    return new SpyInstructionExecutor(routineFinderInstructionSpy1, state);
  }

  @Provides
  @Inject
  @Singleton
  private SymbolicExecutionAdapter getExecutionAdapter(State state1, RoutineManager routineManager, RoutineFinderInstructionSpy spy, DataflowService dataflowService1) {
    return new SymbolicExecutionAdapter(state1, routineManager, spy, dataflowService1);
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

  @Provides
  @Inject
  private TransformerInstructionExecutor getTransformerInstructionExecutor(State state1, SpyInstructionExecutor tInstructionExecutor, InstructionTransformer instructionTransformer) {
    return new TransformerInstructionExecutor<T>(state1.getPc(), tInstructionExecutor, true, instructionTransformer);
  }

  @Provides
  @Inject
  protected OpcodeConditions getOpcodeConditions(State state1) {
    return new OpcodeConditions(state1.getFlag(), state1.getRegister(B));
  }

  @Provides
  @Inject
  protected DataflowService getDataflowService() {
    return new VirtualRegisterDataflowService();
  }

  @Provides
  @Inject
  private RegistersSetter getRegistersSetter(State state1, VirtualRegisterFactory virtualRegisterFactory) {
    return new VirtualRegistersRegistersSetter<>(state1, virtualRegisterFactory);
  }

  @Provides
  @Inject
  public CPUExecutionContext getSecondContext(State state1, RoutineManager routineManager, SpyInstructionExecutor instructionExecutor1, InstructionTransformer instructionTransformer, MutableOpcodeConditions opcodeConditions, InstructionSpy spy, SymbolicExecutionAdapter symbolicExecutionAdapter1) {
    TransformerInstructionExecutor<T> transformerInstructionExecutor1 = new TransformerInstructionExecutor(state1.getPc(), instructionExecutor1, false, instructionTransformer);
    RandomAccessInstructionFetcher randomAccessInstructionFetcher = (address) -> transformerInstructionExecutor1.getInstructionAt(address);
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
    InstructionFetcher instructionFetcher1 = new TransformerInstructionFetcher(state1, transformerInstructionExecutor1);
    OOZ80 z80 = new OOZ80(state1, instructionFetcher1);
    return new CPUExecutionContext<T>(spy, z80, opcodeConditions);
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
  public VirtualRegisterFactory getVirtualRegisterFactory(SpyInstructionExecutor instructionExecutor, RegisterNameBuilder registerNameBuilder, BlocksManager blocksManager) {
    return new VirtualRegisterFactory<>(instructionExecutor, registerNameBuilder, blocksManager);
  }
}

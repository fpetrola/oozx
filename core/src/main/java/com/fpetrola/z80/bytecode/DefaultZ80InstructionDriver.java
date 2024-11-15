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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.Z80InstructionDriver;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.MemorySpy;
import com.fpetrola.z80.spy.SpyRegisterBankFactory;
import com.fpetrola.z80.transformations.InstructionTransformer;
import com.fpetrola.z80.transformations.RegisterNameBuilder;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;

import java.util.function.Supplier;

public abstract class DefaultZ80InstructionDriver<T extends WordNumber> implements Z80InstructionDriver<T> {
  protected RegisterTransformerInstructionSpy registerTransformerInstructionSpy;
  public InstructionExecutor instructionExecutor;

  @Override
  public State<T> getState() {
    return state;
  }

  public State<T> state;
  Z80Cpu<T> z80;
  protected InstructionFetcher instructionFetcher;
  DefaultInstructionFactory new___;
  protected InstructionTransformer<T> instructionCloner;
  public VirtualRegisterFactory virtualRegisterFactory;

  protected abstract InstructionSpy createSpy();

  public DefaultZ80InstructionDriver(RegisterTransformerInstructionSpy registerTransformerInstructionSpy) {
    this.registerTransformerInstructionSpy = registerTransformerInstructionSpy;

    InstructionSpy spy = createSpy();
    instructionExecutor = new SpyInstructionExecutor(spy);
    state = new State(new MockedIO(), new SpyRegisterBankFactory(spy).createBank(), spy.wrapMemory(new MockedMemory(true)));
    instructionExecutor.setMemptr(state.getMemptr());
    InstructionFactory instructionFactory = createInstructionFactory(state);
    virtualRegisterFactory = new VirtualRegisterFactory(instructionExecutor, new RegisterNameBuilder());
    instructionCloner = new InstructionTransformer(instructionFactory, virtualRegisterFactory);
    instructionFetcher = createInstructionFetcher(spy, state, instructionExecutor);
    z80 = new OOZ80(state, instructionFetcher);
    z80.reset();
    instructionFetcher.reset();
    new___ = new DefaultInstructionFactory<>(state);

    spy.reset(state);
  }

  protected abstract InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor instructionExecutor);

  public void step() {
    z80.execute();
  }

  public MockedMemory<T> mem() {
    return (MockedMemory<T>) (state.getMemory() instanceof MemorySpy memorySpy ? memorySpy.getMemory() : state.getMemory());
  }

  public MockedMemory<T> initMem(Supplier<T[]> supplier) {
    mem().init(supplier);
    return mem();
  }

  public RegisterTransformerInstructionSpy getRegisterTransformerInstructionSpy() {
    return registerTransformerInstructionSpy;
  }

  protected InstructionFactory createInstructionFactory(State<T> state) {
    return new DefaultInstructionFactory(state);
  }
}

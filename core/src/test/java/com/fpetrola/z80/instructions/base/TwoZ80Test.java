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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.RandomAccessInstructionFetcher;
import com.fpetrola.z80.instructions.MemoryAccessOpcodeReference;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.AbstractInstructionSpy;
import com.fpetrola.z80.spy.ComplexInstructionSpy;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.*;
import org.junit.Before;

import java.util.function.Function;

import static com.fpetrola.z80.registers.RegisterName.B;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("ALL")
public abstract class TwoZ80Test<T extends WordNumber> extends ContextDriverDelegator<T> {
  private Z80ContextDriver<T> firstContext;
  private Z80ContextDriver<T> secondContext;
  private RegisterTransformerInstructionSpy registerTransformerInstructionSpy = new RegisterTransformerInstructionSpy(new RoutineManager());

  public TwoZ80Test() {
    super(null);

  }

  @Before
  public <T2 extends WordNumber> void setUp() {
    Function<State<T>, OpcodeConditions> stateOpcodeConditionsFunction = state -> new OpcodeConditions(state.getFlag(), state.getRegister(B));

    firstContext = new CPUExecutionContext<T>(registerTransformerInstructionSpy, stateOpcodeConditionsFunction) {
      protected InstructionSpy createSpy() {
        ComplexInstructionSpy spy = new AbstractInstructionSpy<>();
        TraceableWordNumber.instructionSpy = spy;
        return spy;
      }
    };

    Function<State<T>, OpcodeConditions> stateOpcodeConditionsFunction1 = getStateOpcodeConditionsFactory();
    secondContext = new CPUExecutionContext<T>(registerTransformerInstructionSpy, stateOpcodeConditionsFunction1) {
      protected InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor instructionExecutor) {
        TransformerInstructionExecutor<T> instructionExecutor1 = new TransformerInstructionExecutor(this.state.getPc(), this.instructionExecutor, false, (InstructionTransformer) instructionCloner);
        RandomAccessInstructionFetcher randomAccessInstructionFetcher = (address) -> instructionExecutor1.clonedInstructions.get(address);
        registerTransformerInstructionSpy.routineFinder.getRoutineManager().setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
        return buildInstructionFetcher(this.state, instructionExecutor1, spy);
      }

      @Override
      protected RegisterTransformerInstructionSpy createSpy() {
        return registerTransformerInstructionSpy;
      }

      @Override
      protected InstructionFactory createInstructionFactory(State<T> state) {
        return getInstructionFactoryFactory().apply(state);
      }
    };

    useBoth();
    setUpMemory();
  }

  protected Function<State<T>, InstructionFactory> getInstructionFactoryFactory() {
    return state -> new DefaultInstructionFactory(state);
  }

  protected Function<State<T>, OpcodeConditions> getStateOpcodeConditionsFactory() {
    return state -> new OpcodeConditions(state.getFlag(), state.getRegister(B));
  }

  protected InstructionFetcherForTest buildInstructionFetcher(State state, TransformerInstructionExecutor instructionExecutor1, InstructionSpy spy) {
    return new TransformerInstructionFetcher(state, instructionExecutor1);
  }

  protected void useFirst() {
    currentContext = firstContext;
  }

  protected void useSecond() {
    currentContext = secondContext;
  }

  protected void useBoth() {
    currentContext = new ContextDriverMux<T>(firstContext, secondContext);
  }

  protected <J> J assertTypeAndCast(Class<? extends J> expected, Object i1) {
    assertEquals(expected, i1.getClass());
    J ld1 = (J) i1;
    return ld1;
  }

  public void stepFirst() {
    firstContext.step();
  }

  public void stepSecond() {
    secondContext.step();
  }

  protected abstract void setUpMemory();

  protected OpcodeReference mm(ImmutableOpcodeReference<T> c) {
    return new MemoryAccessOpcodeReference(c, this.mem());
  }

  @Override
  public RegisterTransformerInstructionSpy getRegisterTransformerInstructionSpy() {
    return registerTransformerInstructionSpy;
  }
}

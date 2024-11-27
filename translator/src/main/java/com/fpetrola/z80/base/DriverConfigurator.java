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

import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.RandomAccessInstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.TraceableWordNumber;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.AbstractInstructionSpy;
import com.fpetrola.z80.spy.ComplexInstructionSpy;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.*;

import java.util.function.Function;

import static com.fpetrola.z80.registers.RegisterName.B;

public class DriverConfigurator<T extends WordNumber> {

  private Function<State<T>, OpcodeConditions> stateOpcodeConditionsFunction;
  private Function<State<T>, InstructionFactory> instructionFactoryFactory;

  public DriverConfigurator() {
    stateOpcodeConditionsFunction= getStateOpcodeConditionsFactory();
    instructionFactoryFactory= getInstructionFactoryFactory();
  }

  public DriverConfigurator(Function<State<T>, OpcodeConditions> stateOpcodeConditionsFunction1, Function<State<T>, InstructionFactory> stateInstructionFactoryFunction) {
    stateOpcodeConditionsFunction = stateOpcodeConditionsFunction1;
    instructionFactoryFactory = stateInstructionFactoryFunction;
  }

  void configure(final TwoZ80Driver<T> twoZ80Driver) {
    twoZ80Driver.registerTransformerInstructionSpy = new RegisterTransformerInstructionSpy(getRoutineManager());

    twoZ80Driver.firstContext = new CPUExecutionContext<T>(twoZ80Driver.registerTransformerInstructionSpy, state -> new OpcodeConditions(state.getFlag(), state.getRegister(B))) {
      protected InstructionSpy createSpy() {
        ComplexInstructionSpy spy = new AbstractInstructionSpy<>();
        TraceableWordNumber.instructionSpy = spy;
        return spy;
      }
    };

    Function<State<T>, OpcodeConditions> stateOpcodeConditionsFunction1 = stateOpcodeConditionsFunction;
    twoZ80Driver.secondContext = new CPUExecutionContext<T>(twoZ80Driver.registerTransformerInstructionSpy, stateOpcodeConditionsFunction1) {
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
        return instructionFactoryFactory.apply(state);
      }
    };
  }

  protected InstructionFetcherForTest buildInstructionFetcher(State state, TransformerInstructionExecutor instructionExecutor1, InstructionSpy spy) {
    return new TransformerInstructionFetcher(state, instructionExecutor1);
  }
  protected RoutineManager getRoutineManager() {
    return new RoutineManager();
  }

  protected Function<State<T>, InstructionFactory> getInstructionFactoryFactory() {
    return state -> new DefaultInstructionFactory(state);
  }

  protected Function<State<T>, OpcodeConditions> getStateOpcodeConditionsFactory() {
    return state -> new OpcodeConditions(state.getFlag(), state.getRegister(B));
  }
}

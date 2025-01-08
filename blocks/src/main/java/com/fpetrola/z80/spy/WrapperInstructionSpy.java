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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.cache.InstructionCloner;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.List;

public abstract class WrapperInstructionSpy<T extends WordNumber> implements InstructionSpy<T> {
  protected volatile boolean capturing;
  protected ExecutionStep<T> executionStep;
  protected MemorySpy memorySpy;
  protected boolean print = false;
  protected Memory memory;
  protected boolean indirectReference;
  protected State state;
  protected DefaultInstructionFactory instructionFactory;
  protected InstructionCloner instructionCloner;
  private List<ExecutionListener> executionListeners = new ArrayList<>();

  public void reset(State state) {
    InstructionSpy.super.reset(state);
    memory.reset();
  }

  public Memory wrapMemory(Memory aMemory) {
    if (executionStep == null)
      executionStep = new ExecutionStep(memory);
    this.memory = aMemory;
    if (memorySpy == null)
      memorySpy = new MemorySpy(aMemory);

    memorySpy.addMemoryWriteListener((address, value) -> {
      if (isCapturing())
        addWriteMemoryReference((T) address, (T) value);
    });

    memorySpy.addMemoryReadListener((address, value, delta, fetching) -> {
      if (isCapturing())
        addReadMemoryReference((T) address, (T) value);
    });
    return memorySpy;
  }

  public ImmutableOpcodeReference wrapOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    return new OpcodeReferenceSpy(immutableOpcodeReference);
  }

  public Register<T> wrapRegister(Register<T> register) {
    Register<T> result = register;

    if (result instanceof ObservableRegister<T> observableRegister) {
      observableRegister.addRegisterWriteListener(((value, isIncrement) -> {
        if (capturing)
          addWriteReference(register.getName(), (T) value, isIncrement);
      }));
      observableRegister.addRegisterReadListener(((value) -> {
        if (capturing)
          addReadReference(register.getName(), (T) value);
      }));
    }
    return result;
  }

  public void addWriteReference(String opcodeReference, T value, boolean isIncrement) {
    if (capturing) {
      WriteOpcodeReference writeReference = executionStep.addWriteReference(opcodeReference, value, isIncrement, indirectReference);
      if (print)
        System.out.println(writeReference);
    }
  }

  public void addReadReference(String opcodeReference, T value) {
    if (capturing) {
      ReadOpcodeReference readReference = executionStep.addReadReference(opcodeReference, value, indirectReference);
      if (print)
        System.out.println(readReference);
    }
  }

  public void addWriteMemoryReference(T address, T value) {
    if (capturing) {
      WriteMemoryReference writeMemoryReference = executionStep.addWriteMemoryReference(address, value, indirectReference);
      if (print)
        System.out.println(writeMemoryReference);

    }
  }

  public void addReadMemoryReference(T address, T value) {
    if (capturing) {
      ReadMemoryReference<WordNumber> readMemoryReference = (ReadMemoryReference<WordNumber>) executionStep.addReadMemoryReference(address, value, indirectReference);
      if (print)
        System.out.println(readMemoryReference);

    }
  }

  public MemoryPlusRegister8BitReference wrapMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    return new MemoryPlusRegister8BitReferenceSpy(memoryPlusRegister8BitReference);
  }

  public void setState(State state) {
    this.state = state;
    this.memory = state.getMemory();
    instructionFactory = new DefaultInstructionFactory(state);
    instructionCloner = new InstructionCloner(instructionFactory);
  }

  public void addExecutionListener(ExecutionListener executionListener) {
    executionListeners.add(executionListener);
  }

  @Override
  public void beforeExecution(Instruction<T> instruction) {
    executionListeners.forEach(l -> l.beforeExecution(instruction));
  }

  @Override
  public void afterExecution(Instruction<T> instruction) {
    executionListeners.forEach(l -> l.afterExecution(instruction));
  }
}

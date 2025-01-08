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

import com.fpetrola.z80.cpu.DefaultInstructionExecutor;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.registers.Composed16BitRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterBank;

public interface InstructionSpy<T> {
  default void addExecutionListeners(InstructionExecutor<?> defaultInstructionExecutor) {
    defaultInstructionExecutor.addExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
        InstructionSpy.this.beforeExecution(instruction);
      }

      public void afterExecution(Instruction instruction) {
        InstructionSpy.this.afterExecution(instruction);
      }
    });
  }

  default RegisterBank wrapBank(RegisterBank<?> bank) {
    bank.getAll().forEach(r -> {
      if (r instanceof Composed16BitRegister<?, ?> composed16BitRegister) {
        wrapRegister((Register<T>) composed16BitRegister.getLow());
        wrapRegister((Register<T>) composed16BitRegister.getHigh());
      } else {
        wrapRegister(r);
      }
    });
    return bank;
  }

  default Memory<T> wrapMemory(Memory<T> aMemory) {
    return aMemory;
  }

  default ImmutableOpcodeReference<T> wrapOpcodeReference(ImmutableOpcodeReference<T> immutableOpcodeReference) {
    return immutableOpcodeReference;
  }

  default Register<T> wrapRegister(Register<T> register) {
    return register;
  }

  default void beforeExecution(Instruction<T> instruction) {
  }

  default void afterExecution(Instruction<T> instruction) {

  }

  default void enable(boolean enabled) {

  }

  default void flipOpcode(Instruction<T> instruction, int opcodeInt) {

  }

  default MemoryPlusRegister8BitReference wrapMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    return memoryPlusRegister8BitReference;
  }

  default void reset(State state) {
    setState(state);
  }

  default void pause() {
  }

  default void doContinue() {
  }

  default void setState(State state) {
  }

  default boolean isCapturing() {
    return false;
  }

  default void addExecutionListener(ExecutionListener executionListener) {

  }
}

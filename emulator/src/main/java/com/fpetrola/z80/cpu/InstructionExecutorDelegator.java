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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.spy.ExecutionListener;

public interface InstructionExecutorDelegator<T> extends InstructionExecutor<T> {
  default Instruction<T> getInstructionAt(int address) {
    return getDelegate().getInstructionAt(address);
  }

  default Instruction<T> execute(Instruction<T> instruction) {
    return getDelegate().execute(instruction);
  }

  default boolean isExecuting(Instruction<T> instruction) {
    return getDelegate().isExecuting(instruction);
  }

  default void reset() {
    getDelegate().reset();
  }

  default void addExecutionListener(ExecutionListener<T> executionListener) {
    getDelegate().addExecutionListener(executionListener);
  }

  InstructionExecutor<T> getDelegate();

  default void setDelegate(InstructionExecutor<T> delegate) {

  }
}

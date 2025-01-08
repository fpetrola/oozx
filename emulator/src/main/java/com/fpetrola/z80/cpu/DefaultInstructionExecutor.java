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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.google.inject.Inject;

import java.util.*;

public class DefaultInstructionExecutor<T extends WordNumber> implements InstructionExecutor<T> {
  private final Register<T> pc;
  private final Set<Instruction<T>> executingInstructions = new HashSet<>();
  private Map<Integer, Instruction<T>> instructions= new HashMap<>();
  private List<ExecutionListener<T>> executionListeners = new ArrayList<>();

  @Inject
  public DefaultInstructionExecutor(State state) {
    this.pc = state.getPc();
  }

  @Override
  public Instruction<T> getInstructionAt(int address) {
    return instructions.get(address);
  }

  @Override
  public Instruction<T> execute(Instruction<T> instruction) {
    executingInstructions.add(instruction);
    beforeExecution(instruction);
    instruction.execute();
    afterExecution(instruction);
    instructions.put(pc.read().intValue(), instruction);
    executingInstructions.remove(instruction);
    return instruction;
  }

  @Override
  public boolean isExecuting(Instruction<T> instruction) {
    return executingInstructions.contains(instruction);
  }

  @Override
  public void addExecutionListener(ExecutionListener executionListener) {
    executionListeners.add(executionListener);
  }

  public void beforeExecution(Instruction<T> instruction) {
    for (ExecutionListener<T> l : executionListeners) {
      l.beforeExecution(instruction);
    }
  }

  public void afterExecution(Instruction<T> instruction) {
    for (ExecutionListener<T> l : executionListeners) {
      l.afterExecution(instruction);
    }
  }
}

/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DefaultInstructionExecutor implements InstructionExecutor {
  private final Register pc;
  private final Set<Instruction> executingInstructions = new HashSet<>();
  private final Map<java.lang.Integer, Instruction> instructions = new HashMap<>();

  private final boolean skipRepeating;
  private ExecutionListener dummyExecutionListener= new DummyExecutionListener();
  private ExecutionListener executionListener= dummyExecutionListener;

  @Inject
  public DefaultInstructionExecutor(State state, boolean noRepeat) {
    this.pc = state.getPc();
    skipRepeating = noRepeat;
  }

  public Instruction execute(Instruction instruction) {
    executionListener.beforeExecution(instruction);

    if (!(skipRepeating && instruction instanceof RepeatingInstruction))
      instruction.execute();

    executionListener.afterExecution(instruction);

    int nextPC = ((AbstractInstruction) instruction).getNextPC();
    if (nextPC == -1) {
      nextPC = (pc.read() + instruction.getLength()) & 0xFFFF;
    }

    pc.write(nextPC);

    return instruction;
  }

  public void setExecutionListener(ExecutionListener executionListener) {
    this.executionListener = this.executionListener == dummyExecutionListener
        ? executionListener
        : new ExecutionListeners(this.executionListener, executionListener);
  }

  public void addTopExecutionListener(ExecutionListener executionListener) {
    this.executionListener = this.executionListener == dummyExecutionListener
        ? executionListener
        : new ExecutionListeners(executionListener, this.executionListener);
  }

  public boolean isExecuting(Instruction instruction) {
    return executingInstructions.contains(instruction);
  }

  public Instruction getInstructionAt(int address) {
    return instructions.get(address);
  }

  private static class DummyExecutionListener implements ExecutionListener {
    public void beforeExecution(Instruction instruction) {
    }

    public void afterExecution(Instruction instruction) {
    }
  }

  /** Two listeners told one after the other, in the order they were added; a third nests a pair. */
  private record ExecutionListeners(ExecutionListener first, ExecutionListener second) implements ExecutionListener {
    public void beforeExecution(Instruction instruction) {
      first.beforeExecution(instruction);
      second.beforeExecution(instruction);
    }

    public void afterExecution(Instruction instruction) {
      first.afterExecution(instruction);
      second.afterExecution(instruction);
    }
  }
}

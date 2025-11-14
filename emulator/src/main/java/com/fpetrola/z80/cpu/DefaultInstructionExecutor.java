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

import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DefaultInstructionExecutor implements InstructionExecutor {
  private final Register pc;
  private final Set<Instruction> executingInstructions = new HashSet<>();
  private final Map<java.lang.Integer, Instruction> instructions = new HashMap<>();

  private final Consumer<Instruction> afterExecutionAction;
  private ExecutionListener dummyExecutionListener= new DummyExecutionListener();
  private ExecutionListener executionListener= dummyExecutionListener;

  @Inject
  public DefaultInstructionExecutor(State state, boolean noRepeat) {
    this.pc = state.getPc();
    afterExecutionAction = noRepeat ? (instruction1 -> {
      if (instruction1 instanceof RepeatingInstruction repeatingInstruction)
        repeatingInstruction.setNextPC(-1);
    }) : ((a) -> {
    });
  }

  public Instruction execute(Instruction instruction) {
    executionListener.beforeExecution(instruction);

    instruction.execute();

    executionListener.afterExecution(instruction);

    afterExecutionAction.accept(instruction);

    int nextPC = ((AbstractInstruction) instruction).getNextPC();
    if (nextPC == -1) {
      nextPC = (pc.read() + instruction.getLength()) & 0xFFFF;
    }

    pc.write(nextPC);

    return instruction;
  }

  public void setExecutionListener(ExecutionListener executionListener) {
    if (this.executionListener != dummyExecutionListener) {
      CollectionHandler<ExecutionListener> collectionHandler = new CollectionHandler<>();
      collectionHandler.add(this.executionListener);
      collectionHandler.add(executionListener);
      this.executionListener = new ExecutionListener() {
        public void beforeExecution(Instruction instruction) {
          collectionHandler.forAll(item -> item.beforeExecution(instruction));
        }

        public void afterExecution(Instruction instruction) {
          collectionHandler.forAll(item -> item.afterExecution(instruction));
        }
      };
    } else
      this.executionListener = executionListener;
  }

  public void addTopExecutionListener(ExecutionListener executionListener) {
//    executionListeners.add(executionListener);
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
}

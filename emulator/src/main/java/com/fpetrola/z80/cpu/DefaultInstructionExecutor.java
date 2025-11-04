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

import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class DefaultInstructionExecutor<T extends WordNumber> implements InstructionExecutor<T> {
  private final Register<T> pc;
  private final Set<Instruction<T>> executingInstructions = new HashSet<>();
  private final Map<Integer, Instruction<T>> instructions = new HashMap<>();
  private final CollectionHandler<ExecutionListener<T>> executionListeners = new CollectionHandler<>();

  private final Consumer<Instruction<T>> afterExecutionAction;

  @Inject
  public DefaultInstructionExecutor(State<T> state, boolean noRepeat) {
    this.pc = state.getPc();
    afterExecutionAction = noRepeat ? (instruction1 -> {
      if (instruction1 instanceof RepeatingInstruction<?> repeatingInstruction)
        repeatingInstruction.setNextPC(null);
    }) : ((a) -> {
    });
  }

  public Instruction<T> execute(Instruction<T> instruction) {

    executionListeners.forAll(i -> i.beforeExecution(instruction));

    instruction.execute();

    executionListeners.forAll(i -> i.afterExecution(instruction));

    afterExecutionAction.accept(instruction);

    T nextPC = ((AbstractInstruction<T>) instruction).getNextPC();
    if (nextPC == null) {
      WordNumber wordNumber = pc.read();
      int i = instruction.getLength();
      nextPC = (T) (WordNumber) new WordNumber((wordNumber.valueXYZ + i) & 0xFFFF);
    }

    pc.write(nextPC);

    return instruction;
  }

  public void addExecutionListener(ExecutionListener<T> executionListener) {
    executionListeners.add(executionListener);
  }

  public void addTopExecutionListener(ExecutionListener<T> executionListener) {
//    executionListeners.add(executionListener);
  }

  public boolean isExecuting(Instruction<T> instruction) {
    return executingInstructions.contains(instruction);
  }

  public Instruction<T> getInstructionAt(int address) {
    return instructions.get(address);
  }
}

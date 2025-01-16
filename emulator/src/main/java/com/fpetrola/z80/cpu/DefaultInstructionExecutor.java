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

import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.ExecutionListener;
import com.google.inject.Inject;

import java.util.*;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class DefaultInstructionExecutor<T extends WordNumber> implements InstructionExecutor<T> {
  private final Register<T> pc;
  private final State<T> state;
  private final Set<Instruction<T>> executingInstructions = new HashSet<>();
  private Map<Integer, Instruction<T>> instructions = new HashMap<>();
  private List<ExecutionListener<T>> executionListeners = new ArrayList<>();
  private boolean noRepeat;

  @Override
  public void setNoRepeat(boolean noRepeat) {
    this.noRepeat = noRepeat;
  }

  @Inject
  public DefaultInstructionExecutor(State state, boolean noRepeat) {
    this.pc = state.getPc();
    this.state = state;
    this.noRepeat = noRepeat;
  }

  @Override
  public Instruction<T> getInstructionAt(int address) {
    return instructions.get(address);
  }

  @Override
  public Instruction<T> execute(Instruction<T> instruction) {
    try {
      Memory memory = state.getMemory();
      T pcValue = state.getPc().read();
      memory.read(createValue(-1), 1);
      executingInstructions.add(instruction);
      beforeExecution(instruction);
      instruction.execute();
      afterExecution(instruction);
      instructions.put(pc.read().intValue(), instruction);
      executingInstructions.remove(instruction);
      memory.read(createValue(-2), 1);

      if (noRepeat && instruction instanceof RepeatingInstruction repeatingInstruction) {
        repeatingInstruction.setNextPC(null);
      }

      T nextPC = ((AbstractInstruction<T>) instruction).getNextPC();

//      String toString = new ToStringInstructionVisitor<T>().createToString(instruction2);
//      String x = String.format("%04X", pcValue.intValue()) + ": " + toString + " -> " + nextPC;
//      System.out.println(x);

      if (nextPC == null)
        nextPC = pcValue.plus(instruction.getLength());

      state.getPc().write(nextPC);
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }

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
    for (int i = 0, executionListenersSize = executionListeners.size(); i < executionListenersSize; i++)
      executionListeners.get(i).beforeExecution(instruction);
  }

  public void afterExecution(Instruction<T> instruction) {
    for (int i = 0, executionListenersSize = executionListeners.size(); i < executionListenersSize; i++)
      executionListeners.get(i).afterExecution(instruction);
  }
}

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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.Pop;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;

import java.util.function.Consumer;

public class StackAnalyzer<T extends WordNumber> {
  private final State<T> state;
  private Consumer<StackListener> lastEvent;

  public StackAnalyzer(State<T> state) {
    this.state = state;
  }

  public void beforeExecution(Instruction<T> instruction) {
    lastEvent = null;
    InstructionVisitor<T, Object> instructionVisitor = new InstructionVisitor<>() {
      public void visitingPop(Pop pop) {
        var read = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
        if (read instanceof ReturnAddressWordNumber returnAddressWordNumber) {
          lastEvent = l -> l.returnAddressPopped(returnAddressWordNumber, state.getPc().read().intValue());
        }
      }
    };
    instruction.accept(instructionVisitor);
  }

  public void afterExecution(Instruction<T> instruction) {

  }

  public void listenEvents(StackListener stackListener) {
    if (lastEvent != null)
      lastEvent.accept(stackListener);
  }

  public void addExecutionListener(InstructionExecutor instructionExecutor) {
    instructionExecutor.addExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
        StackAnalyzer.this.beforeExecution(instruction);
      }

      public void afterExecution(Instruction instruction) {
        StackAnalyzer.this.afterExecution(instruction);
      }
    });
  }
}

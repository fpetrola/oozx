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

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.spy.InstructionSpy;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpyInstructionExecutor implements InstructionExecutor {
  private final InstructionSpy spy;
  private final Register pc;
  private final Set<Instruction> executingInstructions = new HashSet<>();
  private Map<java.lang.Integer, Instruction> instructions= new HashMap<>();

  @Inject
  public SpyInstructionExecutor(InstructionSpy spy, State state) {
    this.spy = spy;
    this.pc = state.getPc();
  }

  @Override
  public Instruction getInstructionAt(int address) {
    return instructions.get(address);
  }

  @Override
  public Instruction execute(Instruction instruction) {
    spy.beforeExecution(instruction);
    executingInstructions.add(instruction);
    instruction.execute();
    instructions.put(pc.read(), instruction);
    executingInstructions.remove(instruction);
    spy.afterExecution(instruction);
    return instruction;
  }

  @Override
  public boolean isExecuting(Instruction instruction) {
    return executingInstructions.contains(instruction);
  }
}

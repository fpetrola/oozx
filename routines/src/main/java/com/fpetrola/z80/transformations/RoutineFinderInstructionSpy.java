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

import com.fpetrola.z80.blocks.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.ExecutionStep;
import com.fpetrola.z80.spy.WrapperInstructionSpy;
import com.fpetrola.z80.spy.WriteMemoryReference;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class RoutineFinderInstructionSpy<T extends WordNumber> extends WrapperInstructionSpy<T> {
  private List<WriteMemoryReference> writeMemoryReferences = new ArrayList<>();
  private BlocksManager blocksManager;
  private RoutineFinder routineFinder;
  private final RoutineManager routineManager;
  private final List<Instruction<T>> executedInstructions = new ArrayList<>();
  private Instruction<T> lastInstruction;

  private int lastPC;

  @Inject
  public RoutineFinderInstructionSpy(RoutineManager routineManager, BlocksManager blocksManager1, RoutineFinder routineFinder1) {
    this.routineManager = routineManager;
    capturing = false;
    this.blocksManager = blocksManager1;
    this.routineFinder= routineFinder1;
  }

  @Override
  public void reset(State state) {
    super.reset(state);
    routineManager.reset();
    executionStep = new ExecutionStep(memory);
    blocksManager.clear();
    lastInstruction = null;
    lastPC = 0;
    executedInstructions.clear();
    writeMemoryReferences.clear();
  }

  @Override
  public boolean isCapturing() {
    return capturing;
  }

  public void beforeExecution(Instruction<T> instruction) {
    executedInstructions.add(instruction);
    executionStep = new ExecutionStep(memory);
    executionStep.setInstruction(instruction);
    executionStep.description = instruction.toString();
    super.beforeExecution(instruction);
    Register pc = state.getPc();
    T pcValue = (T) pc.read();
    int pcIntValue = pcValue.intValue();
    routineFinder.checkBeforeExecution(instruction, pcIntValue, state);
  }

  @Override
  public void afterExecution(Instruction<T> instruction) {
    Register pc = state.getPc();
    T pcValue = (T) pc.read();
    int pcIntValue = pcValue.intValue();
    int instructionLength = instruction.getLength();
    if (instructionLength > 0) {
      routineFinder.checkExecution(instruction, pcIntValue, state);
      lastInstruction = instruction;
      super.afterExecution(instruction);
      lastPC = pcValue.intValue();
    }

    getWriteMemoryReferences().addAll(executionStep.writeMemoryReferences);
  }

  public void doContinue() {
    capturing = true;
  }

  public List<WriteMemoryReference> getWriteMemoryReferences() {
    return writeMemoryReferences;
  }

  public List<Instruction<T>> getExecutedInstructions() {
    return executedInstructions;
  }
}

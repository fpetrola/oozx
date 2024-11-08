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
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.RoutineFinder;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.ExecutionStep;
import com.fpetrola.z80.spy.WrapperInstructionSpy;
import com.fpetrola.z80.spy.WriteMemoryReference;

import java.util.ArrayList;
import java.util.List;

public class RegisterTransformerInstructionSpy<T extends WordNumber> extends WrapperInstructionSpy<T> {
  public static List<WriteMemoryReference> writeMemoryReferences = new ArrayList<>();

  public static BlocksManager blocksManager = new BlocksManager(new NullBlockChangesListener(), false);
  private Instruction<T> lastInstruction;
  private int lastPC;
  public static RoutineFinder routineFinder;

  public List<Instruction<T>> getExecutedInstructions() {
    return executedInstructions;
  }

  private List<Instruction<T>> executedInstructions = new ArrayList<>();

  public RegisterTransformerInstructionSpy(RoutineManager routineManager) {
    routineFinder = new RoutineFinder(routineManager);
    capturing = true;
    executionStep = new ExecutionStep(memory);
  }

  @Override
  public void reset(State state) {
    super.reset(state);
    executionStep = new ExecutionStep(memory);
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
  }

  @Override
  public void afterExecution(Instruction<T> instruction) {
    Register pc = state.getPc();
    T pcValue = (T) pc.read();
    int pcIntValue = pcValue.intValue();
    int instructionLength = instruction.getLength();
    if (instructionLength > 0) {
      // instructionLength = 1;


      //   System.out.println(pcIntValue + " - " + instruction);

      routineFinder.checkExecution(instruction, pcIntValue);

      //executionTracking(instruction, pcIntValue, pcValue, instructionLength);
      lastInstruction = instruction;
      super.afterExecution(instruction);
      lastPC = pcValue.intValue();
    }

    writeMemoryReferences.addAll(executionStep.writeMemoryReferences);
  }

  private void executionTracking(Instruction<T> instruction, int pcIntValue, T pcValue, int instructionLength) {
    Block foundBlock = blocksManager.findBlockAt(pcIntValue);

    if (foundBlock != null) {
      if (foundBlock instanceof CodeBlockType codeBlockType) {
        Block split = codeBlockType.getBlock().split(pcValue.intValue(), "jump target", CodeBlockType.class);
      } else {


        if (pcIntValue >= 0) {
          Block previousBlock = blocksManager.findBlockAt(pcIntValue - 1);
          if (previousBlock instanceof Block codeBlock)
            if (!codeBlock.isCompleted() && codeBlock.canTake(pcIntValue)) {
              int start = pcIntValue;
              int end = pcIntValue + instructionLength - 1;
              codeBlock.growBlockTo(end);
              foundBlock = codeBlock;
            }
        }
        foundBlock.accept(new ExecutionTracker(instruction, pcIntValue));
      }
    }


    if (lastInstruction instanceof ConditionalInstruction conditionalInstruction) {

      Block nextBlock = blocksManager.findBlockAt(pcIntValue);
      Block previousBlock = blocksManager.findBlockAt(lastPC);
      if (previousBlock != null) {
        ((CodeBlockType) previousBlock.getBlockType()).addNextBlock(nextBlock);
        if (nextBlock.getBlockType() instanceof CodeBlockType)
          ((CodeBlockType) nextBlock.getBlockType()).addPreviousBlock(previousBlock);
      }
    }
  }
}

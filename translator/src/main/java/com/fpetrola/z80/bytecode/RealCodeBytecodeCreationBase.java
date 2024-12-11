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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.base.CPUExecutionContext;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.*;

import java.util.List;

import static java.util.Comparator.comparingInt;

@SuppressWarnings("ALL")
public class RealCodeBytecodeCreationBase<T extends WordNumber> extends CPUExecutionContext<T> implements BytecodeGeneration {
  public RoutineManager routineManager;
  public SymbolicExecutionAdapter symbolicExecutionAdapter;
  private RegistersSetter<T> registersSetter;
  private RandomAccessInstructionFetcher randomAccessInstructionFetcher;

  public RealCodeBytecodeCreationBase(RoutineFinderInstructionSpy routineFinderInstructionSpy1, RoutineManager routineManager1,
                                      SpyInstructionExecutor instructionExecutor1,
                                      SymbolicExecutionAdapter executionAdapter, InstructionTransformer instructionCloner1,
                                      TransformerInstructionExecutor<T> transformerInstructionExecutor1, OOZ80 z80, OpcodeConditions opcodeConditions, RegistersSetter<T> registersSetter1) {
    super(routineFinderInstructionSpy1, z80, opcodeConditions);
    routineManager = routineManager1;

    symbolicExecutionAdapter = executionAdapter;
    randomAccessInstructionFetcher = (address) -> transformerInstructionExecutor1.clonedInstructions.get(address);
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
    registersSetter = registersSetter1;
  }

  public void reset() {
    super.reset();
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
  }

  public List<Routine> getRoutines() {
    List<Routine> routines = routineManager.getRoutines().stream()
        .sorted(comparingInt(Routine::getStartAddress))
        .toList();

    System.out.println("\n\nDetecting routines\n\n");
    routines.forEach(System.out::println);
    return routines;
  }

  public void stepUntilComplete(int startAddress) {
    symbolicExecutionAdapter.stepUntilComplete(this, this.getState(), startAddress, 16384 + 4096);
  }

  @Override
  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  public String generateAndDecompile() {
    return generateAndDecompile("", getRoutines(), ".", "JetSetWilly", symbolicExecutionAdapter);
  }

  @Override
  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    return getDecompiledSource(className, targetFolder, getState(), !base64Memory.isBlank(), this.symbolicExecutionAdapter, base64Memory);
  }


  public void translateToJava(String className, String memoryInBase64, String startMethod) {
    BytecodeGeneration.super.translateToJava(className, startMethod, getState(), !memoryInBase64.isBlank(), symbolicExecutionAdapter, memoryInBase64);
  }

  public RegistersSetter<T> getRegistersSetter() {
    return registersSetter;
  }
}

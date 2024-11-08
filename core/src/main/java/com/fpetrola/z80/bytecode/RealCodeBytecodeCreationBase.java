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

import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.RandomAccessInstructionFetcher;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.instructions.base.InstructionFactory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.transformations.*;

import java.util.List;

import static java.util.Comparator.comparingInt;

@SuppressWarnings("ALL")
public class RealCodeBytecodeCreationBase<T extends WordNumber> extends DefaultZ80InstructionDriver<T> implements BytecodeGeneration {
  public static RoutineManager routineManager = new RoutineManager();
  protected TransformerInstructionExecutor<T> transformerInstructionExecutor;
  private RandomAccessInstructionFetcher randomAccessInstructionFetcher;
  private static SymbolicExecutionAdapter symbolicExecutionAdapter;

  public RealCodeBytecodeCreationBase() {
    super(new RegisterTransformerInstructionSpy(routineManager));
    randomAccessInstructionFetcher = (address) -> transformerInstructionExecutor.clonedInstructions.get(address);
    routineManager.setRandomAccessInstructionFetcher(randomAccessInstructionFetcher);
  }

  public static SymbolicExecutionAdapter getSymbolicExecutionAdapter(State state1) {
    if (symbolicExecutionAdapter == null)
      symbolicExecutionAdapter = new SymbolicExecutionAdapter(state1, routineManager);

    return symbolicExecutionAdapter;
  }

  public static List<Routine> getRoutines() {
    List<Routine> routines = RealCodeBytecodeCreationBase.routineManager.getRoutines().stream()
        .sorted(comparingInt(Routine::getStartAddress))
        .toList();

    System.out.println("\n\nDetecting routines\n\n");
    routines.forEach(System.out::println);
    return routines;
  }

  @Override
  protected InstructionSpy createSpy() {
    return registerTransformerInstructionSpy;
  }

  @Override
  protected InstructionFetcher createInstructionFetcher(InstructionSpy spy, State<T> state, InstructionExecutor instructionExecutor) {
    transformerInstructionExecutor = new TransformerInstructionExecutor(state.getPc(), instructionExecutor, true, (InstructionTransformer) instructionCloner);
    return getSymbolicExecutionAdapter(state).createInstructionFetcher(spy, state, (InstructionExecutor) this.transformerInstructionExecutor);
  }

  @Override
  protected InstructionFactory createInstructionFactory(State<T> state) {
    return getSymbolicExecutionAdapter(state).createInstructionFactory(this.state);
  }

  @Override
  public int add(Instruction<T> instruction) {
    return 0;
  }

  @Override
  public Instruction getInstructionAt(int i) {
    return randomAccessInstructionFetcher.getInstructionAt(i);
  }

  @Override
  public Instruction getTransformedInstructionAt(int i) {
    return null;
  }

  protected void stepUntilComplete(int startAddress) {
    getSymbolicExecutionAdapter(state).stepUntilComplete(this, this.state, startAddress, 16384 + 4096);
  }

  @Override
  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  public String generateAndDecompile() {
    return generateAndDecompile("", getRoutines(), ".", "JetSetWilly");
  }

  @Override
  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className) {
    return getDecompiledSource(state.getPc(), randomAccessInstructionFetcher, className, base64Memory, routines, targetFolder);
  }


  public void translateToJava(String className, String memoryInBase64, String startMethod, List<Routine> routines) {
    BytecodeGeneration.super.translateToJava(state.getPc(), randomAccessInstructionFetcher, className, memoryInBase64, startMethod, routines);
  }

  protected DefaultRegistersSetter<T> getDefaultRegistersSetter() {
    DefaultRegistersSetter<T> registersBase = new DefaultRegistersSetter<>(state) {
      public VirtualRegisterFactory getVirtualRegisterFactory() {
        return virtualRegisterFactory;
      }
    };
    return registersBase;
  }
}

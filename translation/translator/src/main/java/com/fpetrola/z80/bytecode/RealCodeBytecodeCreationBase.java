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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.base.CPUExecutionContext;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.*;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.instructions.impl.Call;

import static java.util.Comparator.comparingInt;

@SuppressWarnings("ALL")
public class RealCodeBytecodeCreationBase extends CPUExecutionContext implements BytecodeGeneration {
  public RoutineManager routineManager;
  public SymbolicExecutionAdapter symbolicExecutionAdapter;
  private final InstructionExecutor instructionExecutor;
  private GameData gameData;
  private int[] programImage;

  public void setProgramImage(int[] programImage) {
    this.programImage = programImage;
  }

  public StackAnalyzer getStackAnalyzer() {
    return stackAnalyzer;
  }

  private final StackAnalyzer stackAnalyzer;
  private RegistersSetter registersSetter;

  public RealCodeBytecodeCreationBase(RoutineFinderInstructionSpy routineFinderInstructionSpy1, RoutineManager routineManager1,
                                      InstructionExecutor instructionExecutor1,
                                      SymbolicExecutionAdapter executionAdapter, InstructionTransformer instructionCloner1,
                                      InstructionExecutor instructionExecutor, OOZ80 z80, OpcodeConditions opcodeConditions,
                                      RegistersSetter registersSetter1, StackAnalyzer stackAnalyzer) {
    super(routineFinderInstructionSpy1, z80, opcodeConditions);
    routineManager = routineManager1;

    symbolicExecutionAdapter = executionAdapter;
    this.instructionExecutor = instructionExecutor;
    this.stackAnalyzer = stackAnalyzer;
    registersSetter = registersSetter1;
  }

  public void reset() {
    super.reset();
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
    symbolicExecutionAdapter.stepUntilComplete(this, this.getState(), startAddress, RemoteZ80Translator.SCREEN_END, 0x10000);
  }

  public void exploreRecording(RemoteZ80Translator.Footprint footprint, int start, int... entries) {
    StackAnalyzer stackAnalyzer = getStackAnalyzer();
    footprint.install(getState().getMemory(), getState().getRegisterSP().read());
    stackAnalyzer.learnFrom(footprint.learned());
    routineManager.pushedReturnSites.addAll(stackAnalyzer.layoutCallSites);
    stackAnalyzer.codeVersions.decodeWith(RemoteZ80Translator.decoder());
    stackAnalyzer.reset(getState());
    routineManager.setSpans(footprint.executed());
    symbolicExecutionAdapter.getMutantAddress().addAll(footprint.modifiedCode());
    routineManager.externalEntries.addAll(footprint.externalEntries());
    stackAnalyzer.nonLocalRets.keySet().forEach(ret -> routineManager.externalEntries.addAll(stackAnalyzer.dynamicInvocation.get(ret)));
    routineManager.externalEntries.addAll(stackAnalyzer.calledThrough.values());
    routineManager.externalEntries.addAll(stackAnalyzer.codeVersions.successors());
    routineManager.externalEntries.add(start);
    stepUntilComplete(start);
    Stream.of(IntStream.of(entries).boxed(), footprint.externalEntries().stream(), stackAnalyzer.dynamicInvocation.values().stream(), stackAnalyzer.calledThrough.values().stream(), stackAnalyzer.codeVersions.successors().stream())
        .flatMap(addresses -> addresses).forEach(this::stepUntilComplete);
    footprint.codeBytes().keySet().stream().sorted().filter(site -> site >= 0x4000 && routineManager.getInstructionAt(site) == null).forEach(site -> {
      routineManager.externalEntries.add(site);
      stepUntilComplete(site);
    });
    translateRomRoutines(footprint.romEntries().stream().filter(entry -> stackAnalyzer.trampolineRegister(entry) == null).mapToInt(Integer::intValue).toArray());
    stackAnalyzer.dataConsumedBy.entries().stream().filter(e -> routineManager.findRoutineAt(e.getKey()) != routineManager.findRoutineAt(e.getValue()) && !routineManager.plantsAContinuation(e.getValue(), stackAnalyzer.pushedValues.get(e.getValue())))
        .forEach(e -> routineManager.externalEntries.addAll(stackAnalyzer.dynamicInvocation.get(e.getKey())));
  }

  public void translateRomRoutines(int... entries) {
    for (int entry : entries)
      symbolicExecutionAdapter.stepUntilComplete(this, this.getState(), entry, 0, 0x4000);
  }

  public void translateCodeVariants(int start, int end, int relocationBase, CodeVersions versions) {
    int variableStart = versions.blockRegions().stream().filter(region -> region[0] >= start && region[1] <= end).findFirst().orElseThrow()[0];
    List<int[]> variants = versions.blockContents(variableStart);
    OOZ80 decoder = com.fpetrola.z80.minizx.emulation.EmulatedMiniZX.createOOZ80(new com.fpetrola.z80.minizx.DefaultMiniZXIO());
    routineManager.forgetCode(relocationBase, relocationBase + variants.size() * (end - start + 1));
    symbolicExecutionAdapter.getMutantAddress().removeIf(address -> address >= variableStart && address < variableStart + variants.get(0).length);
    symbolicExecutionAdapter.routineExecutorHandler.forgetExecutions(relocationBase, relocationBase + variants.size() * (end - start + 1));
    int[] decoderMemory = (int[]) decoder.getState().getMemory().getData();
    int size = end - start;
    List<RoutineManager.CodeVariant> copies = new java.util.ArrayList<>();
    for (int k = 0; k < variants.size(); k++) {
      int[] variable = variants.get(k);
      int[] code = java.util.Arrays.copyOfRange(programImage, start, end);
      System.arraycopy(variable, 0, code, variableStart - start, variable.length);
      int at = relocationBase + k * (size + 1);
      System.arraycopy(code, 0, decoderMemory, start, size);
      for (int address = start; address < end; ) {
        decoder.getState().getPc().write(address);
        int length = decoder.getInstructionFetcher().fetchNextInstruction().getLength();
        int opcode = code[address - start], target = length == 3 ? code[address - start + 1] | code[address - start + 2] << 8 : -1;
        if ((opcode == 0xc3 || opcode == 0xcd || (opcode & 0xc7) == 0xc4 || (opcode & 0xc7) == 0xc2) && target >= start && target < end) {
          code[address - start + 1] = target - start + at & 0xff;
          code[address - start + 2] = target - start + at >> 8;
        }
        address += length;
      }
      for (int i = 0; i < size; i++)
        getState().getMemory().write(at + i, code[i]);
      copies.add(new RoutineManager.CodeVariant(start, end, variableStart, variable, at, code));
    }
    routineManager.codeVariants.addAll(copies);
    getState().getMemory().protect(relocationBase, relocationBase + variants.size() * (size + 1));
    java.util.Set<Integer> explored = new java.util.HashSet<>();
    for (java.util.Set<Integer> entries; !explored.containsAll(entries = routineManager.entriesInto(start, end)); )
      entries.stream().filter(explored::add).toList().forEach(entry -> copies.forEach(v -> stepUntilComplete(v.relocated(entry))));
    explored.forEach(entry -> copies.forEach(v -> routineManager.externalEntries.add(v.relocated(entry))));
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
    return getDecompiledSource(className, targetFolder, getState(), !base64Memory.isBlank(), this.symbolicExecutionAdapter, withCodeVariants(base64Memory), gameData);
  }

  private String withCodeVariants(String base64Memory) {
    if (base64Memory.isBlank() || routineManager.codeVariants.isEmpty())
      return base64Memory;
    byte[] image = Base64Utils.gzipDecompressFromBase64(base64Memory);
    routineManager.codeVariants.forEach(v -> {
      for (int i = 0; i < v.code().length; i++)
        image[v.relocatedAt() + i] = (byte) v.code()[i];
    });
    return Base64Utils.gzipArrayCompressToBase64(image);
  }


  public MiniZX translatedProgram(String className, String memoryInBase64) {
    return translatedProgram(className, getState(), symbolicExecutionAdapter, withCodeVariants(memoryInBase64), gameData);
  }

  public RegistersSetter getRegistersSetter() {
    return registersSetter;
  }

  public void setGameData(GameData gameData) {
    this.gameData = gameData;
  }
}

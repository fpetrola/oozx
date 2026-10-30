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
import com.fpetrola.z80.helpers.Helper;
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

  private final java.util.BitSet readByTheExploration = new java.util.BitSet();

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
    footprint.bankedCode().values().forEach(banked -> banked.instructions().keySet().stream().filter(address -> !footprint.codeBytes().containsKey(address)).forEach(routineManager.bankedOnly::add));
    getStackAnalyzer().codeVersions.mutant().addAll(footprint.modifiedCode());
    routineManager.externalEntries.addAll(footprint.externalEntries());
    routineManager.externalEntries.add(start);
    getState().getMemory().addMemoryReadListener((address, value, fetching) -> readByTheExploration.set(address));
    stepUntilComplete(start);
    Stream.of(IntStream.of(entries).boxed(), footprint.externalEntries().stream(), stackAnalyzer.dynamicInvocation.values().stream(), stackAnalyzer.calledThrough.values().stream(), stackAnalyzer.codeVersions.successors().stream())
        .flatMap(addresses -> addresses).forEach(this::stepUntilComplete);
    footprint.codeBytes().keySet().stream().sorted().filter(site -> site >= 0x4000 && routineManager.getInstructionAt(site) == null).forEach(site -> {
      routineManager.externalEntries.add(site);
      stepUntilComplete(site);
    });
    translateBankedCode(footprint);
    translateRewrittenRegions(stackAnalyzer.codeVersions);
    translateRomRoutines(footprint.romEntries().stream().filter(entry -> stackAnalyzer.trampolineRegister(entry) == null).mapToInt(Integer::intValue).toArray());
  }

  public void translateRomRoutines(int... entries) {
    for (int entry : entries)
      symbolicExecutionAdapter.stepUntilComplete(this, this.getState(), entry, 0, 0x4000);
  }

  /** Every region the program rewrites with a content of its own runs from a copy per recorded content, entered by the hash of what is there. */
  private void translateRewrittenRegions(CodeVersions versions) {
    versions.blockRegions().forEach(region -> {
      List<CodeVersions.Shape> shapes = versions.shapes(region[0]);
      List<RoutineManager.CodeVariant> copies = shapes.size() > 1 ? copiesOf(region[0], region[1], region[0], shapes, 0) : null;
      int at = copies == null ? -1 : freeAreaFor(copies.stream().mapToInt(copy -> copy.code().length).sum());
      if (at != -1) {
        translateCodeVariants(region[0], region[1], at, versions);
        if (Boolean.getBoolean("translation.reportRoutines"))
          System.out.println("REGION " + Helper.formatAddress(region[0]) + "-" + Helper.formatAddress(region[1]) + " copies at " + Helper.formatAddress(at) + (readByTheExploration.get(at, at + copies.stream().mapToInt(copy -> copy.code().length).sum()).isEmpty() ? "" : " over bytes the exploration read"));
      } else if (shapes.size() > 1 && Boolean.getBoolean("translation.reportRoutines"))
        System.out.println("REGION " + Helper.formatAddress(region[0]) + "-" + Helper.formatAddress(region[1]) + " with " + shapes.size() + " shapes stays in place: " + (copies == null ? "a relative jump cannot reach its trampoline" : "no free area for its copies"));
    });
    java.util.Collection<List<RoutineManager.CodeVariant>> regions = routineManager.codeVariants.stream().filter(v -> v.bank() == -1).collect(java.util.stream.Collectors.groupingBy(RoutineManager.CodeVariant::start)).values();
    for (boolean more = true; more; )
      more = regions.stream().map(this::exploreEntriesOf).reduce(false, Boolean::logicalOr);
  }

  public void translateCodeVariants(int start, int end, int relocationBase, CodeVersions versions) {
    int variableStart = versions.blockRegions().stream().filter(region -> region[0] >= start && region[1] <= end).findFirst().orElseThrow()[0];
    List<CodeVersions.Shape> variants = versions.shapes(variableStart);
    List<RoutineManager.CodeVariant> copies = copiesOf(start, end, variableStart, variants, relocationBase);
    if (copies == null)
      throw new IllegalStateException("a relative jump of " + Helper.formatAddress(start) + "-" + Helper.formatAddress(end) + " cannot reach its trampoline");
    int size = copies.stream().mapToInt(copy -> copy.code().length).sum();
    routineManager.forgetCode(relocationBase, relocationBase + size);
    getStackAnalyzer().codeVersions.mutant().removeIf(address -> address >= variableStart && address < variableStart + variants.get(0).bytes().length);
    symbolicExecutionAdapter.routineExecutorHandler.forgetExecutions(relocationBase, relocationBase + size);
    copies.forEach(copy -> {
      for (int i = 0; i < copy.code().length; i++)
        getState().getMemory().write(copy.relocatedAt() + i, copy.code()[i]);
      for (int offset : copy.varying())
        getStackAnalyzer().codeVersions.mutant().add(copy.relocated(variableStart + offset));
    });
    routineManager.codeVariants.addAll(copies);
    getState().getMemory().protect(relocationBase, relocationBase + size);
    while (exploreEntriesOf(copies)) ;
  }

  /** Explores every copy of a region from the entries into the region known so far; true when there were new ones. */
  private boolean exploreEntriesOf(List<RoutineManager.CodeVariant> copies) {
    RoutineManager.CodeVariant first = copies.get(0);
    java.util.function.Function<Integer, List<RoutineManager.CodeVariant>> entering = entry -> copies.stream().filter(copy -> copy.hasInstructionAt(entry)).toList();
    List<Integer> entries = routineManager.entriesInto(first.start(), first.end()).stream().filter(entry -> entering.apply(entry).stream().anyMatch(copy -> !routineManager.externalEntries.contains(copy.relocated(entry)))).toList();
    entries.forEach(entry -> entering.apply(entry).forEach(copy -> stepUntilComplete(copy.relocated(entry))));
    entries.forEach(entry -> entering.apply(entry).forEach(copy -> routineManager.externalEntries.add(copy.relocated(entry))));
    return !entries.isEmpty();
  }

  /** One copy per shape, placed one after another from relocationBase: the region's bytes with the shape's, a JP to the code after the region and, for each relative jump that leaves the region, a JP to its target that the jump is bent to, after the copy or, when that is out of its reach, before it; null when neither is in reach. */
  private List<RoutineManager.CodeVariant> copiesOf(int start, int end, int variableStart, List<CodeVersions.Shape> shapes, int relocationBase) {
    OOZ80 decoder = com.fpetrola.z80.minizx.emulation.EmulatedMiniZX.createOOZ80(new com.fpetrola.z80.minizx.DefaultMiniZXIO());
    int[] decoderMemory = (int[]) decoder.getState().getMemory().getData();
    int size = end - start, at = relocationBase;
    List<RoutineManager.CodeVariant> copies = new java.util.ArrayList<>();
    for (CodeVersions.Shape shape : shapes) {
      int[] body = java.util.Arrays.copyOf(java.util.Arrays.copyOfRange(programImage, start, end), size + 3);
      System.arraycopy(shape.bytes(), 0, body, variableStart - start, shape.bytes().length);
      jumpTo(body, size, end);
      System.arraycopy(body, 0, decoderMemory, start, size);
      List<int[]> instructions = new java.util.ArrayList<>(), before = new java.util.ArrayList<>(), after = new java.util.ArrayList<>();
      for (int address = start; address < end; ) {
        decoder.getState().getPc().write(address);
        int length = decoder.getInstructionFetcher().fetchNextInstruction().getLength(), target = relativeJumpTarget(body, start, address, length);
        instructions.add(new int[]{address, length});
        if (target != -1 && (target < start || target >= end))
          (size + 3 + 3 * after.size() - (address - start + 2) <= 127 ? after : before).add(new int[]{address - start, target});
        address += length;
      }
      int prefix = 3 * before.size(), bodyAt = at + prefix;
      instructions.forEach(instruction -> relocateJump(body, start, end, bodyAt, instruction[0], instruction[1]));
      int[] code = new int[prefix + body.length + 3 * after.size()];
      System.arraycopy(body, 0, code, prefix, body.length);
      for (int j = 0; j < before.size(); j++)
        if (!bend(code, prefix, before.get(j), 3 * j))
          return null;
      for (int j = 0; j < after.size(); j++)
        if (!bend(code, prefix, after.get(j), prefix + size + 3 + 3 * j))
          return null;
      copies.add(new RoutineManager.CodeVariant(start, end, variableStart, shape.bytes(), at, code, -1, shape.varying(), prefix, instructions.stream().mapToInt(instruction -> instruction[0]).toArray()));
      at += code.length;
    }
    return copies;
  }

  /** Bends the relative jump at jump[0] of the body to a JP at trampoline towards jump[1]; false when out of its reach. */
  private static boolean bend(int[] code, int body, int[] jump, int trampoline) {
    int displacement = trampoline - (body + jump[0] + 2);
    if (displacement < -128 || displacement > 127)
      return false;
    code[body + jump[0] + 1] = displacement & 0xff;
    jumpTo(code, trampoline, jump[1]);
    return true;
  }

  private static void jumpTo(int[] code, int offset, int target) {
    code[offset] = 0xC3;
    code[offset + 1] = target & 0xff;
    code[offset + 2] = target >> 8;
  }

  private static int relativeJumpTarget(int[] code, int start, int address, int length) {
    int opcode = code[address - start];
    return length == 2 && (opcode == 0x18 || opcode == 0x10 || (opcode & 0xe7) == 0x20) ? address + 2 + (byte) code[address - start + 1] & 0xffff : -1;
  }

  private static void relocateJump(int[] code, int start, int end, int at, int address, int length) {
    int opcode = code[address - start], target = length == 3 ? code[address - start + 1] | code[address - start + 2] << 8 : -1;
    if ((opcode == 0xc3 || opcode == 0xcd || (opcode & 0xc7) == 0xc4 || (opcode & 0xc7) == 0xc2) && target >= start && target < end) {
      code[address - start + 1] = target - start + at & 0xff;
      code[address - start + 2] = target - start + at >> 8;
    }
  }

  /** Code that ran with another bank paged at C000 is translated from a copy below C000, first in screen memory, which never runs as code, entered when that bank is paged. */
  public void translateBankedCode(RemoteZ80Translator.Footprint footprint) {
    footprint.bankedCode().forEach((bank, banked) -> {
      java.util.Map<Integer, int[]> instructions = banked.instructions();
      int start = java.util.Collections.min(instructions.keySet()), end = instructions.entrySet().stream().mapToInt(e -> e.getKey() + e.getValue().length).max().getAsInt();
      int at = freeAreaFor(end - start);
      if (at == -1)
        throw new IllegalStateException("no free area of " + (end - start) + " bytes below C000 for another bank's code");
      int[] code = java.util.Arrays.copyOfRange(banked.contents(), start - com.fpetrola.z80.memory.MemoryBanks.WINDOW, end - com.fpetrola.z80.memory.MemoryBanks.WINDOW);
      instructions.forEach((address, bytes) -> relocateJump(code, start, end, at, address, bytes.length));
      for (int i = 0; i < code.length; i++)
        getState().getMemory().write(at + i, code[i]);
      getState().getMemory().protect(at, at + code.length);
      RoutineManager.CodeVariant variant = new RoutineManager.CodeVariant(start, end, start, code, at, code, bank, new int[0], 0, instructions.keySet().stream().mapToInt(Integer::intValue).sorted().toArray());
      routineManager.codeVariants.add(variant);
      banked.modified().forEach(address -> getStackAnalyzer().codeVersions.mutant().add(variant.relocated(address)));
      StackAnalyzer stackAnalyzer = getStackAnalyzer();
      java.util.Set<Integer> entries = new java.util.TreeSet<>(routineManager.entriesInto(start, end));
      stackAnalyzer.dynamicInvocation.entries().stream().filter(jump -> instructions.containsKey(jump.getKey())).toList().forEach(jump -> stackAnalyzer.dynamicInvocation.put(variant.relocated(jump.getKey()), jump.getValue()));
      entries.addAll(stackAnalyzer.dynamicInvocation.values());
      entries.removeIf(entry -> !instructions.containsKey(entry));
      entries.forEach(entry -> routineManager.externalEntries.add(variant.relocated(entry)));
      java.util.stream.Stream.concat(entries.stream(), instructions.keySet().stream().sorted())
          .map(variant::relocated).filter(entry -> routineManager.getInstructionAt(entry) == null).forEach(this::stepUntilComplete);
    });
  }

  /** The first gap below C000 that holds a copy of that size, -1 when none does. */
  /** The first gap of {@code size} bytes below the bank window with no code, no protected byte and nothing the exploration read; failing that, one that only keeps clear of code and protected bytes. */
  private int freeAreaFor(int size) {
    int unread = freeAreaFor(size, true);
    return unread != -1 ? unread : freeAreaFor(size, false);
  }

  private int freeAreaFor(int size, boolean unread) {
    java.util.BitSet code = routineManager.codeAddresses();
    int free = com.fpetrola.z80.minizx.SpectrumApplication.ROM_END, address = free;
    for (; address < com.fpetrola.z80.memory.MemoryBanks.WINDOW && address - free < size; address++)
      if (code.get(address) || getState().getMemory().isProtected(address) || unread && readByTheExploration.get(address))
        free = address + 1;
    return address - free < size ? -1 : free;
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
    routineManager.codeVariants.stream().filter(v -> v.bank() == -1).forEach(v -> {
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

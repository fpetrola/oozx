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

package com.fpetrola.z80.routines;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.opcodes.references.ConditionAlwaysTrue;
import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.CodeBlockType;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.transformations.StackAnalyzer;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingInt;

public class RoutineManager {

  public ListValuedMap<Integer, Integer> callers = new ArrayListValuedHashMap<>();
  public ListValuedMap<Integer, Integer> callees = new ArrayListValuedHashMap<>();
  public ListValuedMap<Integer, Integer> jumpsAfterStackReset = new ArrayListValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> returnPoints = new HashSetValuedHashMap<>();
  private final MultiValuedMap<Integer, Integer> siteReturnPoints = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> nonLocalReturns = new HashSetValuedHashMap<>();
  public final MultiValuedMap<Integer, Integer> nonLocalReturnPoints = new HashSetValuedHashMap<>();
  public final java.util.Set<Integer> pushedReturnSites = new java.util.HashSet<>();
  public BlocksManager blocksManager;
  private List<Routine> routines = new ArrayList<>();

  private final Map<Integer, Instruction> instructions = new HashMap<>();

  public RoutineManager() {
    this(new BlocksManager(new NullBlockChangesListener(), true));
  }

  public RoutineManager(BlocksManager blocksManager) {
    this.blocksManager = blocksManager;
  }

  public Routine findRoutineAt(int address) {
    Optional<Routine> first = new ArrayList<>(routines).stream().filter(r -> r != null && r.contains(address)).findFirst();
    if (first.isPresent()) {
      return first.get().findRoutineAt(address);
    } else
      return first.orElse(null);
  }

  public void forgetCode(int from, int to) {
    instructions.keySet().removeIf(address -> address >= from && address < to);
    new ArrayList<>(routines).forEach(routine -> {
      routine.removeBlocks(routine.getBlocks().stream().filter(block -> block.getRangeHandler().getStartAddress() >= from && block.getRangeHandler().getEndAddress() < to).toList());
      if (routine.getBlocks().isEmpty())
        removeRoutine(routine);
    });
  }

  public Routine addRoutine(Routine routine) {
    if (routines.contains(routine))
      System.out.println("dfasfasf!!!!");
    routines.add(routine);
    routine.setRoutineManager(this);
    return routine;
  }

  public List<Routine> getRoutines() {
    return this.routines.stream()
        .sorted(comparingInt(Routine::getStartAddress))
        .toList();
  }

  public void optimizeAll() {
    new ArrayList<>(routines).forEach(Routine::optimize);
  }

  public void createVirtualRoutines() {
    boolean changes;

    do {
      changes = false;
      ArrayList<Routine> routines1 = new ArrayList<>(routines);
      Collections.reverse(routines1);
      for (Routine routine : routines1)
        changes |= routine.splitVirtualRoutines();
    } while (changes);
    routines.removeIf(routine -> routine.getBlocks().isEmpty());
  }

  public Routine createRoutine(int startAddress, int length) {
    Block foundBlock = blocksManager.findBlockAt(startAddress);
    foundBlock.split(startAddress + length - 1);
    foundBlock = foundBlock.split(startAddress - 1, CodeBlockType.class);
    foundBlock.setType(new CodeBlockType());

    return addRoutine(new Routine(foundBlock, startAddress, false));
  }

  public void recordInstruction(int address, Instruction instruction) {
    instructions.put(address, instruction);
  }

  public void addReturnPoint(int callSite, int point) {
    returnPoints.put(((ConditionalInstruction<?>) instructions.get(callSite)).getJumpAddress(), point);
    siteReturnPoints.put(callSite, point);
  }

  public void planNonLocalReturns(StackAnalyzer stackAnalyzer, java.util.Set<Routine> jumpMembers) {
    stackAnalyzer.shiftedReturns.entries().forEach(e -> {
      Routine owner = findRoutineAt(e.getKey());
      Integer continuation = stackAnalyzer.callContinuations.get(e.getValue());
      if (owner != null && continuation != null && !(instructions.get(e.getKey()) instanceof Ret) && instructions.get(e.getValue()) instanceof Call call && !jumpMembers.contains(findRoutineAt(e.getValue()))
          && !(isCode(call.getJumpAddress()) ? List.of(call.getJumpAddress()) : stackAnalyzer.calledThrough.get(e.getValue())).contains(owner.getEntryPoint())) {
        nonLocalReturns.put(e.getKey(), continuation);
        nonLocalReturnPoints.put(e.getValue(), continuation);
      }
    });
    stackAnalyzer.nonLocalRets.entries().stream().filter(ret -> !returnPoints.containsValue(ret.getKey()) && !siteReturnPoints.containsValue(ret.getKey())).forEach(ret -> Stream.concat(stackAnalyzer.returnsConsumedBy.get(ret.getKey()).stream(), stackAnalyzer.returnSlots.get(ret.getValue()).stream())
        .filter(callSite -> instructions.get(callSite) instanceof Call).forEach(callSite -> {
          nonLocalReturns.put(ret.getKey(), addressAfter(callSite));
          nonLocalReturnPoints.put(callSite, addressAfter(callSite));
          pushedReturnSites.add(callSite);
        }));
  }

  public void planPoppedReturnsOfRewrittenCalls(StackAnalyzer stackAnalyzer) {
    stackAnalyzer.poppedCallSites.entries().stream().filter(e -> stackAnalyzer.codeVersions.isVersioned(e.getKey()) && instructions.get(e.getKey()) instanceof Call).forEach(e -> {
      Routine popper = findRoutineAt(e.getValue());
      if (popper != null && instructions.containsKey(e.getValue())) {
        popper.getVirtualPop().put(e.getValue(), e.getValue());
        addReturnPoint(e.getKey(), addressAfter(e.getValue()));
      }
    });
  }

  public MultiValuedMap<Integer, Integer> catchPointsOfCallsIn(Routine routine) {
    MultiValuedMap<Integer, Integer> points = returnPointsOfCallsIn(routine);
    nonLocalReturnPoints.entries().stream().filter(e -> routine.contains(e.getKey())).forEach(e -> points.put(e.getKey(), e.getValue()));
    return points;
  }

  public MultiValuedMap<Integer, Integer> returnPointsOfCallsIn(Routine routine) {
    MultiValuedMap<Integer, Integer> points = new HashSetValuedHashMap<>();
    instructions.forEach((address, instruction) -> {
      if (instruction instanceof Call call && routine.contains(address))
        points.putAll(address, returnPoints.get(call.getJumpAddress()));
      if (instruction instanceof Call && routine.contains(address))
        points.putAll(address, siteReturnPoints.get(address));
    });
    return points;
  }

  public record CodeVariant(int start, int end, int variableStart, int[] variableBytes, int relocatedAt, int[] code) {
    public int hash() {
      return java.util.Arrays.hashCode(variableBytes);
    }

    public java.util.Set<Integer> entries(RoutineManager routineManager) {
      return routineManager.entriesInto(start, end);
    }

    public int relocated(int address) {
      return relocatedAt + address - start;
    }
  }

  public final List<CodeVariant> codeVariants = new ArrayList<>();
  public final java.util.Set<Integer> externalEntries = new java.util.TreeSet<>();
  private int codeStart, codeEnd = 0x10000;
  private Map<Integer, Integer> spans = Map.of();
  private final java.util.Set<Integer> interiors = new java.util.HashSet<>();

  public List<CodeVariant> codeVariantsAt(int address) {
    return codeVariants.stream().filter(v -> v.entries(this).contains(address)).toList();
  }

  public java.util.Set<Integer> entriesInto(int start, int end) {
    java.util.Set<Integer> entries = new java.util.TreeSet<>(List.of(start));
    instructions.forEach((address, instruction) -> {
      if ((address < start || address >= end) && fixedJumpTarget(instruction) >= start && fixedJumpTarget(instruction) < end) {
        entries.add(fixedJumpTarget(instruction));
      }
    });
    return entries;
  }

  public int originalAddress(int address) {
    return codeVariants.stream().filter(v -> address >= v.relocatedAt() && address <= v.relocatedAt() + v.end() - v.start())
        .findFirst().map(v -> v.start() + address - v.relocatedAt()).orElse(address);
  }

  public java.util.Set<Routine> routinesInJumpCycles(java.util.function.IntFunction<java.util.Set<Integer>> dynamicTargets) {
    Map<Routine, java.util.Set<Routine>> next = new HashMap<>();
    instructions.forEach((address, instruction) -> {
      Routine from = findRoutineAt(address);
      if (from == null)
        return;
      java.util.Set<Integer> targets = new java.util.HashSet<>();
      if (instruction instanceof Ret)
        targets.addAll(dynamicTargets.apply(address));
      else if (instruction instanceof ConditionalInstruction<?> jump && !(jump instanceof Call))
        if (jump.getPositionOpcodeReference() instanceof com.fpetrola.z80.registers.Register)
          targets.addAll(dynamicTargets.apply(address));
        else
          targets.add(jump.getJumpAddress());
      if (fallsThrough(instruction))
        targets.add(address + instruction.getLength());
      targets.stream().map(this::findRoutineAt).filter(to -> to != null && to != from).forEach(to -> next.computeIfAbsent(from, k -> new java.util.HashSet<>()).add(to));
    });
    return next.keySet().stream().filter(routine -> reaches(next, routine, routine, new java.util.HashSet<>())).collect(java.util.stream.Collectors.toSet());
  }

  private static boolean reaches(Map<Routine, java.util.Set<Routine>> next, Routine from, Routine target, java.util.Set<Routine> visited) {
    return next.getOrDefault(from, java.util.Set.of()).stream().anyMatch(to -> to == target || visited.add(to) && reaches(next, to, target, visited));
  }

  public void setCodeRange(int codeStart, int codeEnd) {
    this.codeStart = codeStart;
    this.codeEnd = codeEnd;
  }

  public void setSpans(Map<Integer, Integer> spans) {
    this.spans = spans;
    spans.forEach((address, length) -> {
      for (int i = 1; i < length; i++)
        interiors.add(address + i & 0xffff);
    });
    interiors.removeAll(spans.keySet());
  }

  public boolean wasExecuted(int address) {
    return spans.containsKey(address);
  }

  public boolean isCode(int address) {
    return address >= codeStart && address < codeEnd && (originalAddress(address) != address || !interiors.contains(address));
  }

  public boolean isCalledFrom(Routine routine, int callAddress) {
    return !(getInstructionAt(callAddress) instanceof Call call && isCode(call.getJumpAddress()) && !routine.contains(call.getJumpAddress()));
  }

  public boolean isEnteredFromOutside(Routine owner, int address) {
    return owner.getEntryPoint() != address && (jumpsAfterStackReset.get(address).stream().anyMatch(caller -> !owner.contains(caller)) || isFallenIntoFromOutside(owner, address)
        || externalEntries.contains(address) || isJumpedIntoFromOtherRoutine(owner, address) || isReturnPointOfCallFromOutside(owner, address));
  }

  private boolean isReturnPointOfCallFromOutside(Routine owner, int address) {
    return returnPoints.entries().stream().anyMatch(point -> point.getValue() == address
        && instructions.entrySet().stream().anyMatch(e -> e.getValue() instanceof Call call && call.getJumpAddress() == point.getKey() && !owner.contains(e.getKey())))
        || siteReturnPoints.entries().stream().anyMatch(point -> point.getValue() == address && !owner.contains(point.getKey()));
  }

  private boolean isFallenIntoFromOutside(Routine owner, int address) {
    int before = addressBefore(address);
    Routine previousOwner = before == -1 ? null : findRoutineAt(before);
    return previousOwner != null && previousOwner != owner && fallsThrough(getInstructionAt(before));
  }

  public void splitAtEntriesFromOutside() {
    java.util.Set<Integer> candidates = new java.util.TreeSet<>(externalEntries);
    candidates.addAll(callers.keySet());
    candidates.addAll(jumpsAfterStackReset.keySet());
    instructions.values().forEach(instruction -> candidates.add(fixedJumpTarget(instruction)));
    routines.forEach(routine -> routine.getBlocks().forEach(block -> candidates.add(block.getRangeHandler().getStartAddress())));
    for (boolean changed = true; changed; ) {
      changed = false;
      for (int address : candidates) {
        Routine owner = findRoutineAt(address);
        if (owner != null && isEnteredFromOutside(owner, address) && reachesOnlyItsOwnTail(owner, address))
          changed |= owner.splitAt(address);
      }
    }
  }

  private boolean reachesOnlyItsOwnTail(Routine owner, int entry) {
    int end = owner.findBlockOf(entry).getRangeHandler().getEndAddress();
    boolean enteredMidTail = instructions.entrySet().stream().anyMatch(e -> owner.contains(e.getKey()) && (e.getKey() < entry || e.getKey() > end) && fixedJumpTarget(e.getValue()) > entry && fixedJumpTarget(e.getValue()) <= end);
    java.util.Deque<Integer> pending = new java.util.ArrayDeque<>(List.of(entry));
    java.util.Set<Integer> seen = new java.util.HashSet<>();
    while (!enteredMidTail && !pending.isEmpty()) {
      int address = pending.pop();
      if (!seen.add(address) || !owner.contains(address))
        continue;
      if (address < entry || address > end)
        return false;
      Instruction instruction = instructions.get(address);
      if (instruction != null && fixedJumpTarget(instruction) != -1)
        pending.push(fixedJumpTarget(instruction));
      if (instruction != null && fallsThrough(instruction))
        pending.push(addressAfter(address));
    }
    return !enteredMidTail;
  }

  public boolean isJumpedIntoFromOtherRoutine(Routine routine) {
    return isJumpedIntoFromOtherRoutine(routine, routine.getEntryPoint());
  }

  public boolean isJumpedIntoFromOtherRoutine(Routine routine, int entry) {
    Stream<Integer> staticJumps = instructions.entrySet().stream().filter(e -> jumpsTo(e.getValue(), entry)).map(Map.Entry::getKey);
    return Stream.concat(staticJumps, callers.get(entry).stream()).anyMatch(pc -> !routine.contains(pc) && findRoutineAt(pc) != null);
  }

  private static boolean jumpsTo(Instruction instruction, int target) {
    return !(instruction instanceof Call) && fixedJumpTarget(instruction) == target;
  }

  public static int fixedJumpTarget(Instruction instruction) {
    return instruction instanceof ConditionalInstruction<?> jump && !(jump instanceof Ret) && !(jump.getPositionOpcodeReference() instanceof com.fpetrola.z80.registers.Register) ? jump.getJumpAddress() : -1;
  }

  public static boolean fallsThrough(Instruction instruction) {
    if (instruction instanceof Call)
      return true;
    return !(instruction instanceof ConditionalInstruction<?> conditional && conditional.getCondition() instanceof ConditionAlwaysTrue);
  }

  public Instruction getInstructionAt(int address) {
    return instructions.get(address);
  }

  public int addressBefore(int address) {
    for (int length = 1; length <= 4; length++)
      if (getInstructionAt(address - length) != null && getInstructionAt(address - length).getLength() == length)
        return address - length;
    return -1;
  }

  public Stream<Integer> fixedStoreTargets(int[] memory) {
    return instructions.entrySet().stream().flatMap(e -> CodeVersions.fixedStoreTargets(e.getKey(), e.getValue(), memory).stream());
  }

  public int spanOf(int address, Instruction instruction) {
    return instruction instanceof Call ? spans.getOrDefault(address, instruction.getLength()) : instruction.getLength();
  }

  public int addressAfter(int address) {
    return address + instructions.get(address).getLength();
  }

  public void reset() {
    instructions.clear();
    blocksManager.clear();
    routines.clear();
    callees.clear();
    callers.clear();
    jumpsAfterStackReset.clear();
    returnPoints.clear();
    siteReturnPoints.clear();
    nonLocalReturns.clear();
    nonLocalReturnPoints.clear();
    pushedReturnSites.clear();
    codeVariants.clear();
    externalEntries.clear();
    spans = Map.of();
    interiors.clear();
  }

  public void removeRoutine(Routine routine) {
    routines.remove(routine);
  }

  public List<Routine> getRoutinesInDepth() {
    List<Routine> flat = new ArrayList<>();
    List<Routine> routines1 = routines;
    for (Routine r : routines1) {
      List<Routine> allRoutines = r.getAllRoutines();
      boolean disjoint = Collections.disjoint(allRoutines, flat);
//      if (!disjoint)
//        System.out.println("rrrrrr");
      for (Routine routine : allRoutines) {
        if (flat.contains(routine))
          System.out.println("agdgdag");
        flat.add(routine);
      }
    }

    return flat;
  }
}

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

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class CodeVersions implements java.io.Serializable {
  public enum Kind {OPERAND, INSTRUCTION, BLOCK}

  private final Map<Integer, List<int[]>> versions = new TreeMap<>();
  private final Map<Integer, Kind> kinds = new TreeMap<>();
  private final List<int[]> blockRegions = new ArrayList<>();
  private final Map<Integer, List<int[]>> blockContents = new TreeMap<>();
  private final Set<Integer> patched = new TreeSet<>(), mutant = new HashSet<>();
  private final Map<Integer, Integer> executedLengths = new TreeMap<>();
  private final List<int[]> merges = new ArrayList<>();
  private boolean dirty;
  private transient int[] regionAt;
  private transient int[][] lastSeen;
  private transient BiFunction<Integer, int[], Instruction> decoder;

  public void record(int address, int[] first, int[] other) {
    List<int[]> known = versions.computeIfAbsent(address, a -> new ArrayList<>(List.of(first)));
    if (known.stream().noneMatch(v -> Arrays.equals(v, other)))
      known.add(other);
    dirty = true;
  }

  public void patched(Set<Integer> addresses, Map<Integer, int[]> executed) {
    patched.addAll(addresses);
    executed.forEach((address, bytes) -> executedLengths.put(address, bytes.length));
    dirty = true;
    executed.forEach((address, bytes) -> {
      if (IntStream.range(0, bytes.length).anyMatch(i -> patched.contains(Space.next(address, i))))
        record(address, bytes, bytes);
    });
  }

  public void addAll(CodeVersions other) {
    other.versions.forEach((address, known) -> known.forEach(v -> record(address, known.get(0), v)));
    patched.addAll(other.patched);
    executedLengths.putAll(other.executedLengths);
    other.blockContents.forEach((start, contents) -> contents.forEach(content -> addBlockContent(start, content)));
    other.merges.forEach(merge -> mergeBlockRegions(merge[0], merge[1]));
  }

  /** Code bytes that may hold at runtime something other than what was translated: they are read from memory or run through the emulator. */
  public Set<Integer> mutant() {
    return mutant;
  }

  public Map<Integer, List<int[]>> versions() {
    return versions;
  }

  public boolean isVersioned(int address) {
    return versions.containsKey(address);
  }

  public Map<Integer, Kind> kinds() {
    return kinds;
  }

  public List<int[]> instructionVersions(int address) {
    return kinds.get(address) == Kind.INSTRUCTION ? versions.get(address) : List.of();
  }

  public Instruction decode(int address, int[] bytes) {
    Instruction instruction = decoder.apply(address, bytes);
    if (instruction instanceof ConditionalInstruction<?> jump && !(jump instanceof Ret))
      jump.calculateJumpAddress();
    return instruction;
  }

  public Set<Integer> modifiedBytes() {
    Set<Integer> modified = new HashSet<>();
    versions.forEach((address, known) -> known.forEach(v -> {
      for (int i = 0; i < Math.max(v.length, known.get(0).length); i++)
        if (i >= v.length || i >= known.get(0).length || v[i] != known.get(0)[i])
          modified.add(Space.next(address, i));
    }));
    modified.addAll(patched);
    return modified;
  }

  public Set<Integer> successors() {
    Set<Integer> successors = new TreeSet<>();
    kinds.forEach((address, kind) -> instructionVersions(address).forEach(bytes -> {
      Instruction instruction = decode(address, bytes);
      if (RoutineManager.fixedJumpTarget(instruction) != -1)
        successors.add(RoutineManager.fixedJumpTarget(instruction));
    }));
    return successors;
  }

  public List<int[]> blockRegions() {
    return blockRegions;
  }

  public boolean inBlock(int address) {
    return blockRegions.stream().anyMatch(region -> address >= region[0] && address < region[1]);
  }

  /** The regions inside the span become one (a template whose variable parts are apart); the merge is kept for every later build of the regions. */
  public void mergeBlockRegions(int start, int end) {
    if (merges.stream().noneMatch(merge -> merge[0] == start && merge[1] == end))
      merges.add(new int[]{start, end});
    List<int[]> inside = blockRegions.stream().filter(region -> region[0] >= start && region[1] <= end).toList();
    if (inside.size() > 1) {
      blockRegions.removeAll(inside);
      blockRegions.add(new int[]{inside.get(0)[0], inside.get(inside.size() - 1)[1]});
      regionAt = null;
    }
  }

  /** The contents seen in the region starting there, first seen first; only those spanning the region as it is now. */
  public List<int[]> blockContents(int regionStart) {
    int span = blockRegions.stream().filter(region -> region[0] == regionStart).mapToInt(region -> region[1] - region[0]).findFirst().orElse(-1);
    return blockContents.getOrDefault(regionStart, List.<int[]>of()).stream().filter(content -> content.length == span).toList();
  }

  /** Called at every fetch of a replay: only a fetch inside a region looks, and only a content other than the last one seen there is copied. */
  public void recordBlockContent(int address, int[] memory) {
    if (regionAt == null) {
      regionAt = new int[0x10000];
      Arrays.fill(regionAt, -1);
      for (int i = 0; i < blockRegions.size(); i++)
        for (int a = blockRegions.get(i)[0]; a < blockRegions.get(i)[1]; a++)
          regionAt[a & 0xffff] = i;
      lastSeen = new int[blockRegions.size()][];
    }
    int i = regionAt[address & 0xffff];
    if (i < 0 || lastSeen[i] != null && Arrays.equals(memory, blockRegions.get(i)[0], blockRegions.get(i)[1], lastSeen[i], 0, lastSeen[i].length))
      return;
    lastSeen[i] = Arrays.copyOfRange(memory, blockRegions.get(i)[0], blockRegions.get(i)[1]);
    addBlockContent(blockRegions.get(i)[0], lastSeen[i]);
  }

  private void addBlockContent(int start, int[] content) {
    List<int[]> known = blockContents.computeIfAbsent(start, s -> new ArrayList<>());
    if (known.stream().noneMatch(c -> Arrays.equals(c, content)))
      known.add(content);
  }

  /** Classifies the versions; the block regions are rebuilt when versions or executed instructions were added, hold whole instructions of every content, and keep the merges asked for. */
  public void decodeWith(BiFunction<Integer, int[], Instruction> decoder) {
    this.decoder = decoder;
    if (!dirty)
      return;
    dirty = false;
    regionAt = null;
    versions.forEach((address, known) -> kinds.put(address, kindOf(address, known)));
    blockRegions.clear();
    List<Integer> region = new ArrayList<>();
    int end = -1;
    for (int address : versions.keySet()) {
      if (address > end && !region.isEmpty())
        blockIfAnyIs(region, end);
      region.add(address);
      end = Math.max(end, address + versions.get(address).stream().mapToInt(v -> v.length).max().orElse(1));
    }
    if (!region.isEmpty())
      blockIfAnyIs(region, end);
    closeRegions();
    merges.forEach(merge -> mergeBlockRegions(merge[0], merge[1]));
  }

  /** An executed instruction overlapping a region extends it to hold the whole instruction, until nothing more overlaps; regions that meet merge. */
  private void closeRegions() {
    for (boolean changed = true; changed; ) {
      changed = false;
      for (int[] region : blockRegions)
        for (Map.Entry<Integer, Integer> executed : executedLengths.entrySet()) {
          int from = executed.getKey(), to = from + executed.getValue();
          if (from < region[1] && to > region[0] && (from < region[0] || to > region[1])) {
            region[0] = Math.min(region[0], from);
            region[1] = Math.max(region[1], to);
            changed = true;
          }
        }
      blockRegions.sort(java.util.Comparator.comparingInt(region -> region[0]));
      for (int i = 1; i < blockRegions.size(); )
        if (blockRegions.get(i)[0] <= blockRegions.get(i - 1)[1]) {
          blockRegions.get(i - 1)[1] = Math.max(blockRegions.get(i - 1)[1], blockRegions.remove(i)[1]);
          changed = true;
        } else
          i++;
    }
  }

  private void blockIfAnyIs(List<Integer> region, int end) {
    if (region.stream().anyMatch(address -> kinds.get(address) == Kind.BLOCK)) {
      region.forEach(address -> kinds.put(address, Kind.BLOCK));
      blockRegions.add(new int[]{region.get(0), end});
    }
    region.clear();
  }

  private Kind kindOf(int address, List<int[]> known) {
    int[] first = known.get(0);
    if (known.stream().anyMatch(v -> v.length != first.length))
      return Kind.BLOCK;
    Instruction instruction = decode(address, first);
    Set<Integer> operands = operandOffsets(instruction);
    boolean operandsOnly = known.stream().allMatch(v -> IntStream.range(0, v.length).allMatch(i -> v[i] == first[i] || operands.contains(i))) && IntStream.range(0, first.length).filter(i -> patched.contains(Space.next(address, i))).allMatch(operands::contains);
    return operandsOnly && !(instruction instanceof JumpInstruction) ? Kind.OPERAND : Kind.INSTRUCTION;
  }

  public static List<Integer> fixedStoreTargets(int address, Instruction instruction, int[] memory) {
    if (!(instruction instanceof Ld ld && (ld.getTarget() instanceof IndirectMemory8BitReference target ? target.getTarget() : ld.getTarget() instanceof IndirectMemory16BitReference target ? target.getTarget() : null) instanceof Memory16BitReference operand))
      return List.of();
    int at = address + operand.getDelta(), stored = memory[Space.address(at)] | memory[Space.address(at + 1)] << 8;
    return ld.getTarget() instanceof IndirectMemory16BitReference ? List.of(stored, stored + 1 & 0xffff) : List.of(stored);
  }

  public static Set<Integer> operandOffsets(Instruction instruction) {
    Set<Integer> operands = new HashSet<>();
    instruction.accept(new InstructionVisitor<>() {
      public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
        source.accept(this);
      }

      public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
        target.accept(this);
      }

      public boolean visitMemory8BitReference(Memory8BitReference operand) {
        operands.add(operand.getDelta());
        return true;
      }

      public boolean visitMemory16BitReference(Memory16BitReference operand) {
        operands.addAll(List.of(operand.getDelta(), operand.getDelta() + 1));
        return true;
      }

      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
        indirectMemory8BitReference.getTarget().accept(this);
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        indirectMemory16BitReference.getTarget().accept(this);
      }
    });
    return operands;
  }
}

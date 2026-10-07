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

public class CodeVersions {
  public enum Kind {OPERAND, INSTRUCTION, BLOCK}

  private final Map<Integer, List<int[]>> versions = new TreeMap<>();
  private final Map<Integer, Kind> kinds = new TreeMap<>();
  private BiFunction<Integer, int[], Instruction> decoder;

  public void record(int address, int[] first, int[] other) {
    List<int[]> known = versions.computeIfAbsent(address, a -> new ArrayList<>(List.of(first)));
    if (known.stream().noneMatch(v -> Arrays.equals(v, other)))
      known.add(other);
  }

  public void addAll(CodeVersions other) {
    other.versions.forEach((address, known) -> known.forEach(v -> record(address, known.get(0), v)));
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
          modified.add(address + i & 0xffff);
    }));
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

  public void decodeWith(BiFunction<Integer, int[], Instruction> decoder) {
    this.decoder = decoder;
    versions.forEach((address, known) -> kinds.put(address, kindOf(address, known)));
    List<Integer> region = new ArrayList<>();
    int end = -1;
    for (int address : versions.keySet()) {
      if (address > end)
        blockIfAnyIs(kinds, region);
      region.add(address);
      end = Math.max(end, address + versions.get(address).stream().mapToInt(v -> v.length).max().orElse(1));
    }
    blockIfAnyIs(kinds, region);
  }

  private static void blockIfAnyIs(Map<Integer, Kind> kinds, List<Integer> region) {
    if (region.stream().anyMatch(address -> kinds.get(address) == Kind.BLOCK))
      region.forEach(address -> kinds.put(address, Kind.BLOCK));
    region.clear();
  }

  private Kind kindOf(int address, List<int[]> known) {
    int[] first = known.get(0);
    if (known.stream().anyMatch(v -> v.length != first.length))
      return Kind.BLOCK;
    Instruction instruction = decode(address, first);
    Set<Integer> operands = operandOffsets(instruction);
    boolean operandsOnly = known.stream().allMatch(v -> IntStream.range(0, v.length).allMatch(i -> v[i] == first[i] || operands.contains(i)));
    return operandsOnly && !(instruction instanceof JumpInstruction) ? Kind.OPERAND : Kind.INSTRUCTION;
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

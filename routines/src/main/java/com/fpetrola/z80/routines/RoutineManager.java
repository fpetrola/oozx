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

package com.fpetrola.z80.routines;

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.CodeBlockType;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.cpu.RandomAccessInstructionFetcher;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingInt;

public class RoutineManager {
  public ListValuedMap<Integer, Integer> callers = new ArrayListValuedHashMap<>();
  public ListValuedMap<Integer, Integer> callees = new ArrayListValuedHashMap<>();
  public ListValuedMap<Integer, Integer> callers2 = new ArrayListValuedHashMap<>();
  public BlocksManager blocksManager;
  private List<Routine> routines = new ArrayList<>();

  public RandomAccessInstructionFetcher getRandomAccessInstructionFetcher() {
    return randomAccessInstructionFetcher;
  }

  private RandomAccessInstructionFetcher randomAccessInstructionFetcher;

  public RoutineManager(BlocksManager blocksManager) {
    this.blocksManager = blocksManager;
  }

  public RoutineManager() {
    this(new BlocksManager(new NullBlockChangesListener(), true));
  }

  public Routine findRoutineAt(int address) {
    Optional<Routine> first = routines.stream().filter(r -> r.contains(address)).findFirst();
    if (first.isPresent()) {
      return first.get().findRoutineAt(address);
    } else
      return first.orElse(null);
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

  public void optimizeAllSplit() {
    boolean changes = false;

    do {
      changes = false;
      for (Routine routine : new ArrayList<>(routines))
        changes |= routine.optimizeSplit();
    } while (changes);
  }

  public Routine createRoutine(int startAddress, int length) {
    Block foundBlock = blocksManager.findBlockAt(startAddress);
    foundBlock.split(startAddress + length - 1);
    foundBlock = foundBlock.split(startAddress - 1, CodeBlockType.class);
    foundBlock.setType(new CodeBlockType());

    return addRoutine(new Routine(foundBlock, startAddress, false));
  }

  public void setRandomAccessInstructionFetcher(RandomAccessInstructionFetcher randomAccessInstructionFetcher) {
    this.randomAccessInstructionFetcher = randomAccessInstructionFetcher;
  }

  public void reset() {
    blocksManager.clear();
    routines.clear();
    callees.clear();
    callees.clear();
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

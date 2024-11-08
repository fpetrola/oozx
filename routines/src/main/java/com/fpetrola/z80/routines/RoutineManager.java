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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingInt;

public class RoutineManager {
  public static BlocksManager blocksManager;
  List<Routine> routines = new ArrayList<>();
  private int firstAddress;

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
      Optional<Routine> b = first.get().innerRoutines.stream().filter(i -> i != null && i.contains(address)).findFirst();
      if (b.isPresent())
        return b.get();
    }
    return first.orElse(null);
  }

  public Routine addRoutine(Routine routine) {
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
    routines.forEach(Routine::optimize);
  }

  public Routine createRoutine(int startAddress, int length) {
    Block foundBlock = RoutineManager.blocksManager.findBlockAt(startAddress);
    foundBlock.split(startAddress + length - 1);
    foundBlock = foundBlock.split(startAddress - 1, CodeBlockType.class);
    foundBlock.setType(new CodeBlockType());

    return addRoutine(new Routine(foundBlock));
  }

  public void setRandomAccessInstructionFetcher(RandomAccessInstructionFetcher randomAccessInstructionFetcher) {
    this.randomAccessInstructionFetcher = randomAccessInstructionFetcher;
  }
}

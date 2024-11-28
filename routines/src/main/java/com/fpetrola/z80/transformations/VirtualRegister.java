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

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlockType;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.CodeBlockType;
import com.fpetrola.z80.registers.Register;

import java.util.*;
import java.util.stream.Collectors;

public interface VirtualRegister<T> extends Register<T> {
  List<VirtualRegister<T>> getPreviousVersions();

  BlocksManager getBlocksManager();

  default boolean isInitialized(){
    return false;
  }
  boolean usesMultipleVersions();

  void reset();

  void saveData();

  default boolean hasNoPrevious() {
    List<VirtualRegister<T>> previousVersions = getPreviousVersions();
    return /*previousVersions.size() == 1 && */previousVersions.get(0) instanceof InitialVirtualRegister;
  }

  int getAddress();

  Scope getScope();

  List<VirtualRegister<T>> getDependants();

  default int getRegisterLine() {
    String name = getName();
    int i = name.indexOf("_");
    String substring = name.substring(i + 2);
    int a = substring.indexOf("%");
    int endIndex = a != -1 ? a : substring.length();
    i = i != -1 ? Integer.parseInt(substring.substring(0, endIndex)) : 10000000;
    return i;
  }

  default List<VirtualRegister<?>> getAncestorsOf() {
    List<VirtualRegister<?>> result1 = new ArrayList<>();
    result1.add(this);
    getPreviousVersions().stream().filter(r1 -> r1.getRegisterLine() < getRegisterLine()).forEach(r1 -> result1.addAll(r1.getAncestorsOf()));
    return result1;
  }

  default VirtualRegister<?> getParentPreviousVersion() {
//    return getRegisterLine(virtualRegister);
    List<VirtualRegister<?>> ancestorsOf1 = getAncestorsOf();
    Collections.sort(ancestorsOf1, (c1, c2) -> c2.getRegisterLine() - c1.getRegisterLine());

    for (int i = 0; i < ancestorsOf1.size(); i++) {
      int finalI = i;
      long count = ancestorsOf1.stream().filter(r -> r.getName().equals(ancestorsOf1.get(finalI).getName())).count();
      if (count >= getPreviousVersions().size())
        return ancestorsOf1.get(i);
    }
    return getPreviousVersions().stream().min(Comparator.comparingInt((r1) -> r1.getRegisterLine())).get();
//    return ancestorsOf1.stream().map(r -> getRegisterLine(r)).min(Integer::compare).get();
  }

  default VirtualRegister<T> adjustRegisterScope() {
    VirtualRegister<T> result = this;
    List<VirtualRegister<T>> previousVersions = getPreviousVersions();
      VirtualRegister<T> tVirtualRegister = previousVersions.stream().min(Comparator.comparingInt(VirtualRegister::getRegisterLine)).get();
      previousVersions.stream().forEach(r -> tVirtualRegister.getScope().end = Math.max(tVirtualRegister.getScope().end, r.getScope().end));
      tVirtualRegister.getScope().end = Math.max(tVirtualRegister.getScope().end, this.getScope().end);
      result = tVirtualRegister;
//      previousVersions.stream().forEach(r -> r.getScope().start = Math.min(getScope().start, r.getScope().start));
//      previousVersions.stream().forEach(r -> r.getScope().end = Math.max(getScope().end, r.getScope().end));

    Block result2 = getParentPreviousVersion2();
    if (result2 != null) {
      getScope().end = Math.max(result2.getRangeHandler().getEndAddress(), this.getScope().end);
      getScope().start = Math.min(result2.getRangeHandler().getStartAddress(), this.getScope().start);

    }
//    VirtualRegister<?> result2 = getParentPreviousVersion();


    return result;
  }

  default Block getParentPreviousVersion2() {
    List<VirtualRegister<T>> previousVersions = getPreviousVersions();

    List<Block> blocks = previousVersions.stream().map(r ->
        getBlocksManager().findBlockAt(r.getRegisterLine())
    ).collect(Collectors.toList());


    Block block = getDominantOf(blocks);

    return block;
  }

  default Set<Block> computeDominators2(List<Block> allBlocks, Block entryBlock) {
    Map<Block, Set<Block>> dominators = new HashMap<>();

    Set<Block> previous = getPrevious(allBlocks.get(0));
    if (allBlocks.size() > 1) {
      Set<Block> previous2 = getPrevious(allBlocks.get(1));
      boolean b = previous.retainAll(previous2);
    }
    return previous;
  }

  private Set<Block> getPrevious(Block block) {
    BlockType blockType = block.getBlockType();
    Set<Block> previous = new HashSet<>();
    if (blockType instanceof CodeBlockType codeBlockType)
      previous.addAll(codeBlockType.getPreviousBlocks());
    return previous;
  }

  // Method to compute the dominators for all blocks
  default Map<Block, Set<Block>> computeDominators(List<Block> allBlocks, Block entryBlock) {
    Map<Block, Set<Block>> dominators = new HashMap<>();
    Set<Block> allBlocksSet = new HashSet<>(allBlocks);

    // Initialize dominators for all blocks
    for (Block block : allBlocks) {
      dominators.put(block, new HashSet<>(allBlocksSet));
    }

    // The entry block is only dominated by itself

    getDominatorsOf(entryBlock, dominators).clear();
    getDominatorsOf(entryBlock, dominators).add(entryBlock);

    boolean changed = true;
    while (changed) {
      changed = false;
      for (Block block : allBlocks) {
        if (block.equals(entryBlock)) continue;

        Set<Block> newDominators = new HashSet<>(allBlocksSet);

        BlockType blockType = block.getBlockType();
        if (blockType instanceof CodeBlockType codeBlockType)
          for (Block pred : codeBlockType.getPreviousBlocks()) {
            newDominators.retainAll(getDominatorsOf(pred, dominators));
          }
        newDominators.add(block);

        if (!newDominators.equals(getDominatorsOf(block, dominators))) {
          dominators.put(block, newDominators);
          changed = true;
        }
      }
    }

    return dominators;
  }

  private Set<Block> getDominatorsOf(Block pred, Map<Block, Set<Block>> dominators) {
    if (dominators.get(pred) == null)
      dominators.put(pred, new HashSet<>());
    return dominators.get(pred);
  }

  default Block getDominantOf(List<Block> blocks) {
    Set<Block> blockSetMap = computeDominators2(blocks, getBlocksManager().findBlockAt(0));
    return blockSetMap.isEmpty() ? null : blockSetMap.iterator().next();
  }


  default Integer getMinLineNumber2() {
    return getPreviousVersions().stream().map(r -> r.getRegisterLine()).min(Integer::compare).get();
  }

  default boolean isMixRegister() {
    return getName().contains(",");
  }

  default boolean isComposed() {
    return false;
  }

  default boolean isComposed2() {
    return false;
  }

  default void setComposed(boolean composed) {
  }

  VirtualRegisterVersionHandler getVersionHandler();
}

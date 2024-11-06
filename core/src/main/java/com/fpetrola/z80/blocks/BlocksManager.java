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

package com.fpetrola.z80.blocks;

import java.util.ArrayList;
import java.util.List;

public class BlocksManager {
  List<Block> blocks = new ArrayList<>();
  BlockChangesListener blockChangesListener;
  private long executionNumber;
  private Block[] blocksAddresses = new Block[0x10000];
  private boolean romEnabled;

  public int getCycle() {
    return cycle;
  }

  private int cycle;

  public BlocksManager(BlockChangesListener blockChangesListener, boolean romEnabled) {
    this.blockChangesListener = new BlockChangesListenerDelegator(blockChangesListener) {
      public void blockChanged(Block block) {
        updateBlockAddresses(block);
        super.blockChanged(block);
      }
    };
    this.romEnabled = romEnabled;

    clear();
  }

  public Block findBlockAt(int address) {
    Block block = blocksAddresses[address & 0xFFFF];
    if (block == null)
      System.out.println("dagdsg");
    return block;
  }

  public void addBlock(Block block) {
    blockChangesListener.addingBlock(block);
    blocks.add(block);

    updateBlockAddresses(block);
  }

  private void updateBlockAddresses(Block block) {
    block.getRangeHandler().forEachAddress(address -> {
//      if (blocksAddresses[address] != null)
//        throw new RuntimeException("block already present");
      blocksAddresses[address] = block;
    });
  }

  public void removeBlock(Block block) {
    blockChangesListener.removingBlock(block);
    block.getRangeHandler().forEachAddress(address -> blocksAddresses[address] = null);
    blocks.remove(block);
  }

  public BlockChangesListener getBlockChangesListener() {
    return blockChangesListener;
  }

  public List<Block> getBlocks() {
    return new ArrayList<Block>(blocks);
  }

  public long getExecutionNumber() {
    return executionNumber;
  }

  public void setExecutionNumber(long executionNumber) {
    this.executionNumber = executionNumber;
  }

  public List<Block> getBlocksBetween(int startAddress, int endAddress) {
    List<Block> result = new ArrayList<>();
    Block current = findBlockAt(startAddress);

    while (!current.contains(endAddress)) {
      result.add(current);
      current = current.getRangeHandler().getNextBlock();
    }
    result.add(current);

    return result;
  }

  public void clear() {
    blocks.clear();
    blocksAddresses = new Block[0x10000];
    Block block = new DefaultBlock(0, 0xFFFF, "WHOLE_MEMORY", this);
    block.setType(new UnknownBlockType());
    addBlock(block);
    if (romEnabled)
      block.split(16383, UnknownBlockType.class);
  }
}

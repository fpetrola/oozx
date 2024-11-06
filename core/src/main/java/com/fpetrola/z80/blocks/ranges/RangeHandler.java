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

package com.fpetrola.z80.blocks.ranges;

import com.fpetrola.z80.blocks.*;
import com.fpetrola.z80.instructions.Call;
import com.fpetrola.z80.instructions.base.Instruction;

import java.util.function.Consumer;

public class RangeHandler {
  protected String blockName;

  public int getStartAddress() {
    return startAddress;
  }

  protected int startAddress;

  public int getEndAddress() {
    return endAddress;
  }

  public void setStartAddress(int startAddress) {
    this.startAddress = startAddress;
  }

  public void setEndAddress(int endAddress) {
    this.endAddress = endAddress;
  }

  protected int endAddress;

  public Block getNextBlock() {
    return nextBlock;
  }

  public Block getPreviousBlock() {
    return previousBlock;
  }

  protected Block nextBlock = new NullBlock();
  protected Block previousBlock = new NullBlock();
  protected RangeChangeListener rangeChangeListener;

  public RangeHandler(int start, int end, String typeName, RangeChangeListener rangeChangeListener) {
    checkRange(start, end);
    this.startAddress = start;
    this.endAddress = end;
    blockName = typeName;
    this.rangeChangeListener = rangeChangeListener;
  }

  private static void checkRange(int start, int end) {
    if (start > end)
      throw new RuntimeException("start cannot be greater than end");
  }

  public String getName() {
    return toString();
  }

  public String toString() {
    return String.format("%04d : %04d", startAddress, endAddress);
  }

  public boolean contains(int address) {
    return address >= startAddress && address <= endAddress;
  }

  public <T extends Block> T splitRange(T block, Block aBlock, int address) {
    int lastEndAddress = endAddress;
    checkRange(startAddress, address);
    endAddress = address;

    Block lastNextBlock = nextBlock;

    RangeHandler blockRangeHandler = block.getRangeHandler();
    nextBlock.getRangeHandler().previousBlock= block;
    nextBlock = block;

    blockRangeHandler.nextBlock = lastNextBlock;
    blockRangeHandler.previousBlock = aBlock;
    return block;
  }

  public Block createBlockForSplit(String callType, Class<? extends BlockType> type, Block aBlock, int address) {
    Block block = aBlock.createBlock(Math.min(address + 1, endAddress), endAddress, callType, type);
    return block;
  }

  public void joinRange(Block block, Block otherBlock) {
    Block lastNextBlock = otherBlock.getRangeHandler().nextBlock;
    this.nextBlock = lastNextBlock;
    lastNextBlock.getRangeHandler().previousBlock = block;

    checkRange(startAddress, otherBlock.getRangeHandler().endAddress);
    this.endAddress = otherBlock.getRangeHandler().endAddress;
  }

  public Block replaceRange(Class<? extends BlockType> type, Block aBlock) {
    Block lastPreviousBlock = previousBlock;
    Block lastNextBlock = nextBlock;

    Block block = aBlock.createBlock(startAddress, endAddress, aBlock.getCallType(), type);
    RangeHandler newBlockRangeHandler = block.getRangeHandler();

    previousBlock.getRangeHandler().nextBlock = block;
    nextBlock.getRangeHandler().previousBlock = block;
    newBlockRangeHandler.nextBlock = lastNextBlock;
    newBlockRangeHandler.previousBlock = lastPreviousBlock;
    return block;
  }

  public void chainedJoin(Block startBlock, int end) {
    while (true) {
      RangeHandler rangeHandler = startBlock.getRangeHandler();
      if (!(rangeHandler.endAddress != end - 1)) break;

      Class<? extends BlockType> type1 = startBlock.getBlockType().getClass();
      Class<? extends BlockType> type2 = rangeHandler.nextBlock.getBlockType().getClass();
      if (type1 != type2 && (type1 != UnknownBlockType.class && type2 != UnknownBlockType.class))
        System.out.println("oh!");
      startBlock.join(rangeHandler.nextBlock);
    }
  }

  public boolean isAdjacent(Block block) {
    return endAddress + 1 == block.getRangeHandler().startAddress;
  }

  public boolean isAdjacent(int pcValue) {
    return pcValue == endAddress + 1;
  }

  public Block retrieveAppropriatedBlock(int pcValue, int length, Block fromBlock) {
    Block previousBlock = this.previousBlock;
    Block block = previousBlock;
    if (!previousBlock.canTake(pcValue)) {
      block = fromBlock.growBlockTo(pcValue + length);
    }
    return block;
  }

  public void joinAdjacentIfRequired(int pcValue, Instruction instruction, Block codeBlock) {
    if (nextBlock instanceof CodeBlockType) {
      boolean isRetBlock = nextBlock.getRangeHandler().endAddress - nextBlock.getRangeHandler().startAddress == 0;
      boolean isFromSameRoutine = instruction instanceof Call && pcValue + instruction.getLength() - 1 == endAddress;
      if (isRetBlock || isFromSameRoutine) {
        codeBlock.join(nextBlock);
      }
    }
  }

  public void forEachAddress(Consumer<Integer> consumer) {
    for (int i = startAddress; i <= endAddress; i++)
      consumer.accept(i);
  }
}
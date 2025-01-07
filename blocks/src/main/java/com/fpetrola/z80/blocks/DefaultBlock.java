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

import com.fpetrola.z80.blocks.ranges.RangeHandler;
import com.fpetrola.z80.blocks.references.BlockRelation;
import com.fpetrola.z80.blocks.references.ReferencesHandler;
import com.fpetrola.z80.helpers.Helper;

import java.util.List;

public class DefaultBlock implements Block {
  protected ReferencesHandler referencesHandler;
  protected RangeHandler rangeHandler;
  protected String callType;
  protected BlocksManager blocksManager;
  private BlockType blockType;
  private boolean completed;

  public DefaultBlock() {
  }

  public DefaultBlock(int startAddress, int endAddress, String callType, BlocksManager blocksManager) {
    this();
    init(startAddress, endAddress, blocksManager);
    this.setCallType(callType);
  }

  public DefaultBlock(int start, int end, String call, BlocksManager blocksManager, BlockType blockType) {
    this(start, end, call, blocksManager);
    setType(blockType);
  }

  public DefaultBlock(int startAddress, int endAddress, BlocksManager blocksManager) {
    this();
    init(startAddress, endAddress, blocksManager);
    this.setCallType("");
  }


  @Override
  public void init(int start, int end, BlocksManager blocksManager) {
    rangeHandler = new RangeHandler(start, end, this.getTypeName(), rangeHandler -> blocksManager.blockChangesListener.blockChanged(DefaultBlock.this));
    this.blocksManager = blocksManager;
    referencesHandler = new ReferencesHandler(this);
  }

  @Override
  public Block split(int address, String callType, Class<? extends BlockType> type) {
    check1();
    if (rangeHandler.contains(address) && address < getRangeHandler().getEndAddress()) {
      String lastName = rangeHandler.getName();
      Block blockForSplit = rangeHandler.createBlockForSplit(callType, type, this, address);
      List<BlockRelation> newBlockRelations = referencesHandler.splitReferences(blockForSplit);
      Block block = rangeHandler.splitRange(blockForSplit, this, address);
      getBlocksManager().addBlock(block);
      blockForSplit.getReferencesHandler().addBlockRelations(newBlockRelations);
      getBlocksManager().blockChangesListener.blockChanged(this);

      log("Splitting block: " + lastName + " in: " + rangeHandler.getName() + " -> " + block.getName());
      return block;
    } else
      return this;
  }

  public void log(String lastName) {
    //System.out.println(lastName);
  }

  @Override
  public Block join(Block block) {
    if (block == null) {
      throw new IllegalArgumentException("Block to join cannot be null.");
    }
    if (block.getRangeHandler().getStartAddress() < this.getRangeHandler().getEndAddress() ||
        block.getRangeHandler().getEndAddress() < this.getRangeHandler().getStartAddress()) {
      throw new IllegalArgumentException("Block to join is not adjacent.");
    }

    rangeHandler.joinRange(this, block);
    referencesHandler.joinReferences(block);
    getBlocksManager().removeBlock(block);
    getBlocksManager().blockChangesListener.blockChanged(this);
    log("Joining routine: " + this + " -> " + block);
    return block;
  }

  @Override
  public RangeHandler getRangeHandler() {
    return rangeHandler;
  }

  @Override
  public String getName() {
    return rangeHandler.getName();
  }

  @Override
  public boolean isReferencing(Block block) {
    return referencesHandler.isReferencing(block);
  }

  @Override
  public String getCallType() {
    return callType;
  }

  @Override
  public String toString() {
    return /*getBlockType().toString() + ": " +*/ rangeHandler.toString();
  }

  @Override
  public String getTypeName() {
    return blockType != null ? blockType.getName() : "Block";
  }

  @Override
  public Block createBlock(int startAddress, int endAddress, String callType, Class<? extends BlockType> type) {
    BlockType blockType = Helper.createInstance(type);
    DefaultBlock block = new DefaultBlock();
    block.setType(blockType);
    block.init(startAddress, endAddress, blocksManager);
    return block;
  }

  @Override
  public void setCallType(String callType) {
    this.callType = callType;
  }

  @Override
  public BlocksManager getBlocksManager() {
    return blocksManager;
  }

  @Override
  public void setBlocksManager(BlocksManager blocksManager) {
    this.blocksManager = blocksManager;
  }

  @Override
  public Block joinBlocksBetween(Block aBlock, int end) {
    Block endBlock = blocksManager.findBlockAt(end);
    Block endBlockLess1 = blocksManager.findBlockAt(end - 1);

    if (endBlock == endBlockLess1) {
      Class<? extends BlockType> newBlock = endBlock instanceof UnknownBlockType ? UnknownBlockType.class : CodeBlockType.class;
      Block endSplit = endBlock.split(end - 1, "", newBlock);
    }

    rangeHandler.chainedJoin(aBlock, end);
    return aBlock;
  }

  public boolean canTake(int pcValue) {
    return rangeHandler.isAdjacent(pcValue);
  }

  @Override
  public Block getAppropriatedBlockFor(int pcValue, int length1, BlockType type) {
//    throw new RuntimeException("Cannot jump inside this type of block");
    return this;
  }

  @Override
  public Block replaceType(BlockType type) {
    setType(type);
//    Block block = rangeHandler.replaceRange(type, this);
//    blocksManager.removeBlock(this);
//    referencesHandler.copyReferences(block);
////    blocksManager.replace(this, block);
//    blocksManager.addBlock(block);
    return this;
  }

  @Override
  public boolean contains(int address) {
    return getRangeHandler().contains(address);
  }

  @Override
  public boolean isAdjacent(Block block) {
    return getRangeHandler().isAdjacent(block);
  }

  @Override
  public ReferencesHandler getReferencesHandler() {
    return referencesHandler;
  }

  @Override
  public boolean isReferencedBy(Block block) {
    return referencesHandler.isReferencedBy(block);
  }

  @Override
  public BlockType getBlockType() {
    return blockType;
  }

  @Override
  public void setType(BlockType blockType) {
    this.blockType = blockType;
    blockType.setBlock(this);
  }

  @Override
  public boolean isCompleted() {
    return completed;
  }

  @Override
  public void setCompleted(boolean b) {
    this.completed = b;
  }
}

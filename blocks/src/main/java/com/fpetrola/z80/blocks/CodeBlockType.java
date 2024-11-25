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

import com.fpetrola.z80.instructions.types.Instruction;

import java.util.ArrayList;
import java.util.List;

public class CodeBlockType extends AbstractBlockType {
  public Integer routine;
  private final List<Instruction> instructions = new ArrayList<>();
  private final List<Block> nextBlocks = new ArrayList<>();
  private final List<Block> previousBlocks = new ArrayList<>();

  public CodeBlockType() {
  }

  public CodeBlockType(Block block) {
    this.block = block;
  }

  public Block getAppropriatedBlockFor(int pcValue, int length1, Class<? extends BlockType> type) {
    return block;
  }

  public void addInstruction(Instruction instruction) {
    instructions.add(instruction);
  }

  @Override
  public void accept(BlockRoleVisitor blockRoleVisitor) {
    blockRoleVisitor.visiting(this);
  }

  public List<Block> getPreviousBlocks() {
    return previousBlocks;
  }

  public void addNextBlock(Block block) {
    this.nextBlocks.add(block);
  }

  public void addPreviousBlock(Block block) {
    this.previousBlocks.add(block);
  }

  public List<Block> getNextBlocks() {
    return nextBlocks;
  }
}

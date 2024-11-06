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

public class BlockChangesListenerDelegator implements BlockChangesListener {
  private final BlockChangesListener blockChangesListener;
  private boolean delegationEnabled= true;

  public BlockChangesListenerDelegator(BlockChangesListener blockChangesListener) {

    this.blockChangesListener = blockChangesListener;
  }

  public void removingKnownBlock(Block block, Block calledBlock) {
    if (delegationEnabled)
      blockChangesListener.removingKnownBlock(block, calledBlock);
  }

  public void addingKnownBLock(Block block, Block calledBlock, int from) {
    if (delegationEnabled)
      blockChangesListener.addingKnownBLock(block, calledBlock, from);
  }

  public void removingBlock(Block block) {
    if (delegationEnabled)
      blockChangesListener.removingBlock(block);
  }

  public void addingBlock(Block block) {
    if (delegationEnabled)
      blockChangesListener.addingBlock(block);
  }

  public void blockChanged(Block block) {
    if (delegationEnabled)
      blockChangesListener.blockChanged(block);
  }

  public void replaceBlock(Block oldBlock, Block newBlock) {
    if (delegationEnabled)
      blockChangesListener.replaceBlock(oldBlock, newBlock);
  }
}

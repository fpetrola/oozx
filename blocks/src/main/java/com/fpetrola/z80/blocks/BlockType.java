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

import com.fpetrola.z80.blocks.spy.Nameable;

public interface BlockType extends Nameable {
  Block getBlock();

  void accept(BlockRoleVisitor blockRoleVisitor);

  void setBlock(Block block);

  default String getName() {
    return getClass().getSimpleName().replace("BlockType", "");
  }
}

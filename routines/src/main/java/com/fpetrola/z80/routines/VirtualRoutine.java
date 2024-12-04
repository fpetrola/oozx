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

import java.util.List;

public class VirtualRoutine extends Routine {

  public VirtualRoutine(List<Block> blocks, int entryPoint) {
    super(blocks, entryPoint);
    extracted(entryPoint);
  }

  private static void extracted(int entryPoint) {
    if (entryPoint == 0xEEF1)
      System.out.println("61169 (0xEEF1)");
  }

  public VirtualRoutine(Block block, int entryPoint) {
    super(block, entryPoint);
    extracted(entryPoint);
  }
}

/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.instructions.cache.InstructionCache;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;

public class CachedInstructionFetcher extends DefaultInstructionFetcher {
  protected InstructionCache instructionCache;

  public CachedInstructionFetcher(State aState, InstructionFactory instructionFactory, boolean clone) {
    super(aState, instructionFactory, clone, false);
    instructionCache = new InstructionCache(aState.getMemory(), new DefaultInstructionFactory(aState));
  }

  public Instruction fetchNextInstruction() {
    pcValue = state.getPc().read();
    InstructionCache.CacheEntry cacheEntry = instructionCache.getCacheEntryAt(pcValue);
    Instruction result = super.fetchNextInstruction();
    if (cacheEntry == null)
      instructionCache.cacheInstruction(pcValue, result);
    return result;
  }

  public void reset() {
    super.reset();
    instructionCache.reset();
  }
}

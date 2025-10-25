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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.cache.InstructionCache;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.PhaseInterceptor;

public class CachedInstructionFetcher<T extends WordNumber> extends DefaultInstructionFetcher<T> {
  protected InstructionCache<T> instructionCache;
  private Instruction<T> cached;

  public CachedInstructionFetcher(State<T> aState, InstructionFactory<T> instructionFactory, boolean clone) {
    super(aState, instructionFactory, clone, false);
    instructionCache = new InstructionCache<>(aState.getMemory(), new DefaultInstructionFactory<>(aState));
  }

  public Instruction<T> fetchNextInstruction() {
    pcValue = state.getPc().read();
    Instruction<T> result;

    InstructionCache<T>.CacheEntry cacheEntry = instructionCache.getCacheEntryAt(pcValue);
    if (cacheEntry != null && !cacheEntry.isMutable()) {
      cached = cacheEntry.getInstruction();
      result = cached;
      result = super.fetchNextInstruction();
//      result = cacheEntry.getOpcode();
    } else {
      cached = null;
      result = super.fetchNextInstruction();
      if (cacheEntry == null || !cacheEntry.isMutable())
        instructionCache.cacheInstruction(pcValue, result);
    }
    return result;
  }

  protected void setupPhaseInterceptor(AbstractInstruction<T> fetchedInstruction) {
    if (cached != null) {
      PhaseInterceptor phaseInterceptor = cached.getPhaseInterceptor();
      tPhaseProcessor.setPhase(phaseInterceptor);
      fetchedInstruction.setPhaseInterceptor(phaseInterceptor);
    } else
      super.setupPhaseInterceptor(fetchedInstruction);
  }

  public void reset() {
    super.reset();
    instructionCache.reset();
  }
}

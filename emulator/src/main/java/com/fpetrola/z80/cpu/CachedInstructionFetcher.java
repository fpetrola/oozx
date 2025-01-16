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

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.types.JumpInstruction;
import com.fpetrola.z80.instructions.cache.InstructionCache;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class CachedInstructionFetcher<T extends WordNumber> extends DefaultInstructionFetcher<T> {
  protected InstructionCache<T> instructionCache;

  public CachedInstructionFetcher(State aState, InstructionExecutor<T> instructionExecutor) {
    super(aState, new DefaultInstructionFactory(aState), false, false, false);
    instructionCache = new InstructionCache(aState.getMemory(), new DefaultInstructionFactory(aState));
  }

  public Instruction<T> fetchNextInstruction() {
    pcValue = state.getPc().read();

    InstructionCache.CacheEntry cacheEntry = instructionCache.getCacheEntryAt(pcValue);
    if (cacheEntry != null && !cacheEntry.isMutable()) {
      Instruction<T> instruction = cacheEntry.getOpcode();
//      instructionExecutor.execute(instruction);

      T nextPC = null;
      if (instruction instanceof JumpInstruction jumpInstruction)
        nextPC = (T) jumpInstruction.getNextPC();

      if (nextPC == null)
        nextPC = pcValue.plus(getBaseInstruction(instruction).getLength());

      state.getPc().write(nextPC);
    } else {
      super.fetchNextInstruction();
      if (cacheEntry == null || !cacheEntry.isMutable())
        instructionCache.cacheInstruction(pcValue, this.currentInstruction);
    }
    return null;
  }

  public void reset() {
    super.reset();
    instructionCache.reset();
  }
}

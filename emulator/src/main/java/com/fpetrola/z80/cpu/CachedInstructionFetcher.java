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

import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.impl.Exx;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.RL;
import com.fpetrola.z80.instructions.impl.RST;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.cache.InstructionCache;
import com.fpetrola.z80.minizx.emulation.ToStringInstructionVisitor;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class CachedInstructionFetcher<T extends WordNumber> extends DefaultInstructionFetcher<T> {
  protected InstructionCache<T> instructionCache;

  public CachedInstructionFetcher(State state) {
    super(state, new DefaultInstructionFactory(state), false, false);
    init(state);
  }

  private void init(State aState) {
    instructionCache = new InstructionCache(aState.getMemory(), instructionFactory);
  }

  public <T extends WordNumber> CachedInstructionFetcher(State<T> state, OpcodeConditions opcodeConditions, InstructionFactory instructionFactory1, boolean clone, boolean prefetch) {
    super(state, opcodeConditions, instructionFactory1, clone, prefetch);
    init(state);
  }

  public Instruction<T> fetchNextInstruction() {
    pcValue = state.getPc().read();

    InstructionCache.CacheEntry cacheEntry = instructionCache.getCacheEntryAt(pcValue);

    if (cacheEntry != null && !cacheEntry.isMutable()) {
      registerR.write(createValue(registerR.read().intValue() + cacheEntry.getRdelta()));
      return (Instruction<T>) cacheEntry.getOpcode();
    } else {
      Instruction<T> instruction = super.fetchNextInstruction();
      if (cacheEntry == null || !cacheEntry.isMutable()) {
//        System.out.printf("cloning: %s %s %n", new ToStringInstructionVisitor<>().createToString((Instruction<WordNumber>) instruction), Helper.formatAddress(pcValue.intValue()));
        instructionCache.cacheInstruction(pcValue, this.currentInstruction, rdelta);
      }
      return instruction;
    }
  }

  public void afterExecute(Instruction<?> currentInstruction) {
  }

  public void reset() {
    super.reset();
    instructionCache.reset();
  }
}

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

package com.fpetrola.z80.instructions.cache;

import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.List;

public class InstructionCache<T extends WordNumber> {

  public class MutableOpcode extends CacheEntry {

    public MutableOpcode() {
    }

    public boolean isMutable() {
      return true;
    }
  }

  MutableOpcode mutableOpcode = new MutableOpcode();

  public class CacheEntry {

    private Instruction<T> opcode;

    public CacheEntry() {
    }

    public CacheEntry(Instruction<T> opcode) {
      this.opcode = opcode;
    }

    public Instruction<T> getOpcode() {
      return opcode;
    }

    public boolean isMutable() {
      return false;
    }

  }

  public class InstructionCacheInvalidator implements Runnable {
    private final T pcValue;
    private final int length;

    private InstructionCacheInvalidator(T pcValue, int length) {
      this.pcValue = pcValue;
      this.length = length;
    }

    public void run() {
      for (int j = 0; j < length; j++) {
        opcodesCache.set(pcValue.intValue() + j, mutableOpcode);
        cacheInvalidators[pcValue.intValue() + j] = null;
      }
    }

    public void set() {
      for (int j = 0; j < length; j++)
        cacheInvalidators[pcValue.intValue() + j] = this;
    }
  }

  private List<CacheEntry> opcodesCache = getCacheEntries();

  private List<CacheEntry> getCacheEntries() {
    List<CacheEntry> cacheEntries = new ArrayList<>(0x10000);
    for (int i = 0; i < 0x10000; i++) {
      cacheEntries.add(null);
    }
    return cacheEntries;
  }

  private final Runnable[] cacheInvalidators = new Runnable[0x10000];

  private final InstructionCloner instructionCloner;

  public InstructionCache(Memory memory, InstructionFactory instructionFactory) {
    instructionCloner = new InstructionCloner(instructionFactory);
    memory.addMemoryWriteListener(new CacheInvalidatorMemoryWriteListener(cacheInvalidators));
  }

  public void cacheInstruction(T pcValue, Instruction<T> instruction) {
    Instruction<T> clone = instructionCloner.clone(instruction);
    opcodesCache.set(pcValue.intValue(), new CacheEntry(clone));
    new InstructionCacheInvalidator(pcValue, instruction.getLength()).set();
  }

  public void reset() {
    opcodesCache = getCacheEntries();
  }

  public CacheEntry getCacheEntryAt(T pcValue) {
    if (opcodesCache.size() <= pcValue.intValue())
      return null;
    else
      return opcodesCache.get(pcValue.intValue());
  }
}

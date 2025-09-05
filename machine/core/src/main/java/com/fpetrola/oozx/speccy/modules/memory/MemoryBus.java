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

package com.fpetrola.oozx.speccy.modules.memory;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The address space: the machine's own slots underneath, and over them whatever a device has
 * plugged in, the later over the earlier. An access goes to the range that covers the address.
 * <p>
 * Written as the definition: every access asks. See {@link DecodedMemoryBus} for the same bus
 * answering from what it remembered.
 */
public class MemoryBus {
  /** Who shares these memories and makes an access to them wait. */
  public MemoryContention contention = MemoryContention.NONE;
  private final MappedMemory[] slots = new MappedMemory[4];
  @SuppressWarnings("unchecked")
  private final Map<MemoryPart, MappedMemory>[] slotRanges = new Map[]{new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>()};
  private final List<MappedMemory> plugged = new ArrayList<>();

  /** The machine's own memory in one of its four 16K slots. */
  public void slot(int address, MappedMemory range) {
    slots[address >>> 14] = range;
    changed(address, 0x4000);
  }

  /** The same, for a whole memory from its start: the range is kept, since a machine pages the same banks in and out thousands of times a second. */
  public void slot(int address, MemoryPart memory) {
    slot(address, slotRanges[address >>> 14].computeIfAbsent(memory, bank -> new MappedMemory(address, bank)));
  }

  /** A device holding /ROMCS: its ranges are on the bus from now on, over whatever was there. */
  public void plug(MappedMemory... ranges) {
    for (MappedMemory range : ranges) {
      plugged.remove(range);
      plugged.add(range);
      changed(range.address(), range.length());
    }
  }

  public void unplug(MappedMemory... ranges) {
    for (MappedMemory range : ranges) {
      if (plugged.remove(range)) {
        changed(range.address(), range.length());
      }
    }
  }

  /** Every device gone, as when the machine changes. */
  public void unplugAll() {
    plugged.clear();
    changed(0, 0x10000);
  }

  /** Who answers a read there. */
  public MappedMemory reading(int address) {
    for (int i = plugged.size() - 1; i >= 0; i--) {
      MappedMemory range = plugged.get(i);
      if (range.readable() && range.covers(address)) {
        return range;
      }
    }
    return slots[address >>> 14];
  }

  public MappedMemory writing(int address) {
    for (int i = plugged.size() - 1; i >= 0; i--) {
      MappedMemory range = plugged.get(i);
      if (range.writable() && range.covers(address)) {
        return range;
      }
    }
    return slots[address >>> 14];
  }

  /** Whether the ULA shares the memory that answers there. */
  public boolean contended(int address) {
    return reading(address).memory().contended;
  }

  /** A read the way the processor makes it: what the memory costs, and what it holds. */
  public int read(int address) {
    MappedMemory range = reading(address);
    if (range.memory().contended) {
      contention.beforeRead();
    }
    return range.read(address);
  }

  /** The same byte with no clock and no contention: what a debugger or a snapshot sees. */
  public int peek(int address) {
    return reading(address).read(address);
  }

  public void write(int address, byte value) {
    MappedMemory range = writing(address);
    if (range.memory().contended) {
      contention.beforeWrite();
    }
    range.write(address, value);
  }

  /** A write with no clock and no contention: what a snapshot or a poke puts in. */
  public void poke(int address, byte value) {
    writing(address).write(address, value);
  }

  /** Who covers those addresses may have changed: something plugged in or out there, or a slot set. A bus that remembers hears it here. */
  protected void changed(int address, int length) {
  }
}

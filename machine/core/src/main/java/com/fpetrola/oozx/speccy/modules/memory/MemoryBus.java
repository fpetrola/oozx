/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.modules.memory;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The address space: four machine slots, overlaid by plugged-in device ranges (most recent wins). Scans on every access; {@link DecodedMemoryBus} caches the same lookup.
 */
public class MemoryBus {
  public MemoryContention contention = MemoryContention.NONE;
  private final MappedMemory[] slots = new MappedMemory[4];
  @SuppressWarnings("unchecked")
  private final Map<MemoryPart, MappedMemory>[] slotRanges = new Map[]{new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>()};
  private final List<MappedMemory> plugged = new ArrayList<>();

  public void slot(int address, MappedMemory range) {
    slots[address >>> 14] = range;
    changed(address, 0x4000);
  }

  /** Ranges are cached per bank in {@link #slotRanges}: machines page the same banks in and out constantly. */
  public void slot(int address, MemoryPart memory) {
    slot(address, slotRanges[address >>> 14].computeIfAbsent(memory, bank -> new MappedMemory(address, bank)));
  }

  /** For a device asserting /ROMCS, taking priority over whatever the range already covered. */
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

  public void unplugAll() {
    plugged.clear();
    changed(0, 0x10000);
  }

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

  public boolean contended(int address) {
    return reading(address).memory().contended;
  }

  public int read(int address) {
    MappedMemory range = reading(address);
    if (range.memory().contended) {
      contention.beforeRead();
    }
    return range.read(address);
  }

  /** Unlike {@link #read}, does not cost clock cycles or trigger contention. */
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

  /** Unlike {@link #write}, does not cost clock cycles or trigger contention. */
  public void poke(int address, byte value) {
    writing(address).write(address, value);
  }

  /** Hook for {@link DecodedMemoryBus} to invalidate its cache after a plug, unplug, or slot change. */
  protected void changed(int address, int length) {
  }
}

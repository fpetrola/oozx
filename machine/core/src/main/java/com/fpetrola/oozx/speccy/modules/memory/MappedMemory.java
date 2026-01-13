/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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

/**
 * A memory mapped into the address space at {@code address} for {@code length} bytes, starting at offset {@code from} in it.
 * E.g. the Interface 1 maps its 8K ROM twice, at 0x0000 and 0x2000. A non-readable range still lets the underlying machine memory show through, which is how IDE boards load.
 */
public record MappedMemory(int address, MemoryPart memory, int from, int length, boolean readable, boolean writable) {
  public MappedMemory(int address, MemoryPart memory) {
    this(address, memory, 0, memory.size());
  }

  public MappedMemory(int address, MemoryPart memory, int from, int length) {
    this(address, memory, from, length, true, true);
  }

  public boolean covers(int at) {
    return at >= address && at < address + length;
  }

  public int offset(int at) {
    return at - address + from;
  }

  public int read(int at) {
    return memory.read(offset(at));
  }

  public void write(int at, byte value) {
    memory.write(offset(at), value);
  }
}

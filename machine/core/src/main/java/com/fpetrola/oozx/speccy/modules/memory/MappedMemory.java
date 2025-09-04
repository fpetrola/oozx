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

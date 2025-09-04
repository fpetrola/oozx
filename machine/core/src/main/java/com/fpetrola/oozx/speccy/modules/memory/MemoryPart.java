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
 * Something that holds bytes, or answers as if it did: a ROM, a bank of RAM, a device's
 * registers. Asked with an offset into itself; where it sits on the bus is a {@link MappedMemory}'s
 * business. The kinds are a closed set, which is what lets a generator write a call on one as a
 * branch per kind.
 */
public sealed abstract class MemoryPart permits Storage, Registers {
  /** Whether the ULA shares it, so an access to it waits for the beam. A fact of this memory, not of its kind. */
  public boolean contended;
  /** Which of its kind it is, for whoever compares the map with another emulator's. */
  public int pageNum;

  public abstract int size();

  public abstract int read(int offset);

  public abstract void write(int offset, byte value);
}

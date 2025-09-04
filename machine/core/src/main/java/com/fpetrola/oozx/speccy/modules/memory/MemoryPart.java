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
 * Something addressable as bytes: a ROM, a RAM bank, or a device's registers. Where it sits on the bus is {@link MappedMemory}'s concern.
 * Sealed to {@link Storage} and {@link Registers} so a generator can dispatch on the closed set of kinds.
 */
public sealed abstract class MemoryPart permits Storage, Registers {
  /** Set per instance, not per kind: whether an access here stalls for the ULA. */
  public boolean contended;
  public int pageNum;

  public abstract int size();

  public abstract int read(int offset);

  public abstract void write(int offset, byte value);
}

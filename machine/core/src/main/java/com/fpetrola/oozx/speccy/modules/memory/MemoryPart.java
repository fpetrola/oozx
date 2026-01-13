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

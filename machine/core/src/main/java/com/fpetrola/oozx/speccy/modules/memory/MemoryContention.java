/*
 *
 *  * Copyright (c) 2023-2024 Fernando Petrola
 *  *
 *  *  This program is free software: you can redistribute it and/or modify
 *  *  it under the terms of the GNU General Public License as published by
 *  *  the Free Software Foundation, either version 3 of the License, or
 *  *  (at your option) any later version.
 *  *
 *  *  This program is distributed in the hope that it will be useful,
 *  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *  GNU General Public License for more details.
 *  *
 *  *  You should have received a copy of the GNU General Public License
 *  *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.oozx.speccy.modules.memory;

/**
 * Whoever else uses a memory the machine shares, told before an access reaches it so that it can
 * make that access wait. On a Spectrum it is the ULA, busy fetching the picture; the bus does not
 * know that, and a memory nothing shares leaves it alone.
 */
public interface MemoryContention {
  MemoryContention NONE = new MemoryContention() {
    public void beforeRead() {
    }

    public void beforeWrite() {
    }
  };

  void beforeRead();

  void beforeWrite();
}

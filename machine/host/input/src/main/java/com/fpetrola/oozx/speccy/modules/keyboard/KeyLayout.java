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

package com.fpetrola.oozx.speccy.modules.keyboard;

import com.fpetrola.oozx.speccy.modules.input.Input;

/**
 * What the keys of whoever is typing produce on the Spectrum. A layout, because there is more
 * than one: the keys where they are on a PC, and the Recreated ZX, whose keyboard sends a letter
 * per Spectrum key and needs the ones before it to say which.
 */
public interface KeyLayout {
  /** What that key of the host puts down, or {@link Combination#NONE} for one this layout ignores. */
  Combination produces(Input.InputKey pressed);

  /** Told of every press before {@link #produces}, for a layout that reads a key in what came before it. */
  default void pressed(Input.InputKey key) {
  }

  /** Whether that key is released by pressing it again rather than by letting go, as the Recreated does. */
  default boolean releasedByPressing(Input.InputKey key) {
    return false;
  }
}

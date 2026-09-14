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

package com.fpetrola.oozx.speccy.modules.display;

/**
 * A chip that gives a machine colours of its own instead of the sixteen everybody shares.
 * <p>
 * Here because a snapshot is a machine as it stood, and the colours it stood in are part of that:
 * whoever puts a snapshot back has to be able to say "and these were the colours" without knowing
 * which chip it is talking to, and nothing else in the way this emulator is laid out lets it.
 */
public interface ColoursOfItsOwn {
  /** Whether the machine has the chip at all, which is one of the things a snapshot says about it. */
  void fitted(boolean modified);

  /** The colours as they were, and whether the machine was painting in them at the time. */
  void asItWas(int[] colours, boolean painting);
}

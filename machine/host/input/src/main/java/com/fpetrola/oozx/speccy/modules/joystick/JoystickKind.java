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

package com.fpetrola.oozx.speccy.modules.joystick;

/**
 * A kind of joystick: what pushing it does. The Spectrum had no port of its own for one, so
 * every maker solved it differently - some added a port, and some wired the stick to keys of the
 * machine's own keyboard - which is why this is a kind and not a flag.
 */
public interface JoystickKind {
  /** @return whether this took the push, so whoever pushed does not also send it to the keyboard */
  boolean push(Direction direction, boolean pushed);

  /** What its port reads, for the kinds that have one. */
  default byte reads() {
    return 0;
  }
}

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

package com.fpetrola.oozx.speccy.modules.input;

/** Typing a text on the machine's keyboard, as a person would, for whoever has words to put in. */
public interface Typing {
  /** The key that types this character, if the Spectrum has one; a new line is ENTER. */
  java.util.Optional<Input.InputKey> keyFor(char c);

  /** Each character pressed and let go in turn, with the shifts it needs; the caller does not wait. */
  void type(String text);
}

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

package com.fpetrola.oozx.speccy.machine;

/**
 * A ROM a machine needs is missing or is not the size it should be.
 * <p>
 * Its own type rather than a general failure because loading a bank catches it on purpose: a
 * model asks for the ROM the settings name and falls back to the one it shipped with, and the
 * catch has to be narrow enough that a real fault on the first attempt is not swallowed as a
 * reason to try the second.
 */
public class RomNotLoadedException extends RuntimeException {

  public RomNotLoadedException(String message) {
    super(message);
  }
}

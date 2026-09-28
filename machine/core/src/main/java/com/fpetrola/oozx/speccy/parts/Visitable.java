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

package com.fpetrola.oozx.speccy.parts;

/**
 * A part of the machine that presents itself when the machine is walked: the processor, the
 * memory, the paging, a peripheral. It says what it is by being it, and nothing about what the
 * walk is for - a file format, a description, a debugger - so that none of those ever has to be
 * written into a part.
 */
public interface Visitable {

  /** Presents itself, and after itself whatever parts of its own it has. */
  void accept(PartVisitor visitor);
}

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

package com.fpetrola.oozx.config;

/**
 * Something that comes back as it was when the environment starts again: it says what it is now,
 * and is told the same later. Where that goes and in what form - a machine's file, the
 * environment's own - is the environment's business, and can change without anything that keeps
 * hearing of it.
 * <p>
 * Found where it is, not registered: on a machine (its parts, its peripherals, its window and what
 * is clipped to it) or on the desk. Not a way in: what implements it is made by whoever owns it.
 */
public interface Keeps {
  /** What it is kept as: the same from one version to the next, and unique among what is kept beside it. */
  String keptAs();

  void keep(Kept into);

  void restore(Kept from);
}

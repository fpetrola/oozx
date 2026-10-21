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

import java.util.List;

/**
 * Values with names, and nothing about how they are written: what is put is read back the same,
 * and what was never put reads as whatever the reader asks for. Groups go inside groups, and a
 * name can hold a list of them - the disks in the drives, the windows on a machine.
 * <p>
 * What {@link Keeps} something writes and reads through this, and never learns the form it took.
 */
public interface Kept {
  Kept put(String name, String value);

  Kept put(String name, long value);

  Kept put(String name, double value);

  Kept put(String name, boolean value);

  Kept put(String name, byte[] value);

  String text(String name, String otherwise);

  long number(String name, long otherwise);

  double real(String name, double otherwise);

  boolean flag(String name, boolean otherwise);

  /** What was put under that name, or null. */
  byte[] bytes(String name);

  /** The group under that name, made the first time it is asked for. */
  Kept in(String name);

  /** The groups added under that name, in the order they were added. */
  List<Kept> all(String name);

  /** One more group under that name. */
  Kept add(String name);

  boolean has(String name);
}

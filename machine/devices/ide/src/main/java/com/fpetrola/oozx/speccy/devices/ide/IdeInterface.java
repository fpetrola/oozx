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
package com.fpetrola.oozx.speccy.devices.ide;

import java.io.File;
import java.io.IOException;

/**
 * A mass storage interface as the desk sees it: some slots that take images, and a line about
 * the rest of its state - which memory it has paged where - for the window to show. The IDE
 * boards and the MMC ones differ in what is in the slot, and that is the slot's business.
 */
public interface IdeInterface {
  int units();

  MassStorage drive(int unit);

  void insert(int unit, File image) throws IOException;

  void eject(int unit);

  /** Whether its memory is over the machine's just now, for a lamp; false for a board without any. */
  boolean isPaged();

  /** Its registers, in words, or an empty string. */
  String status();
}

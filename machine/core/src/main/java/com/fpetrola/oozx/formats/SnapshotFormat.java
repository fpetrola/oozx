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

package com.fpetrola.oozx.formats;

import com.fpetrola.emulation.helpers.snapshots.SnapshotException;
import com.fpetrola.oozx.Speccy;
import dev.crystal.plugins.api.RoleInterface;

import java.io.File;
import java.util.function.Consumer;

/**
 * A snapshot format, read into the machine and written out of it by walking it. A way in: the
 * formats live in plugins, and the emulator only knows that one may read a file.
 */
@RoleInterface
public interface SnapshotFormat {

  /** Whether this is the format of that file. */
  boolean reads(File file);

  /**
   * The file put into the machine: the machine it names, and to each part what the file has of
   * it. What could not be put anywhere is said to the notes. A file refused leaves the machine as
   * it was.
   */
  void read(byte[] file, Speccy machine, Consumer<String> notes) throws SnapshotException;

  /** Whether it writes files with that name. */
  default boolean writes(File file) {
    return false;
  }

  /** The machine as a file of this format; what the format has no room for is said to the notes. */
  default byte[] write(Speccy machine, Consumer<String> notes) throws SnapshotException {
    throw new SnapshotException(label() + " is not written");
  }

  /** What the format is called, for saying which one a file is. */
  default String label() {
    return getClass().getSimpleName();
  }
}

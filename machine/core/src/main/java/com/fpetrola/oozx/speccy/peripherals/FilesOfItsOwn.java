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


package com.fpetrola.oozx.speccy.peripherals;

/**
 * A device that has files of its own beside the one a snapshot came from.
 * <p>
 * Here because a snapshot is a file somewhere, and not all of them come alone: the colours of a
 * game in 256 colours are in a file with the same name, and so would be a list of pokes. Whoever
 * loads a snapshot knows the path and has nothing to do with it, and what it should hand the path
 * to is not something it can be made to name.
 */
public interface FilesOfItsOwn {
  /** Where the snapshot came from, so that whoever keeps something beside it can go and look. */
  void beside(String url);
}

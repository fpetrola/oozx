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

package com.fpetrola.oozx.speccy.devices;

import java.io.File;

/**
 * A window that can be handed a file: a cassette deck given a tape, a player given a recording.
 * <p>
 * Which files it takes is the {@link Equipment} offering it, because that is asked before there
 * is a window to ask.
 */
public interface Opens {

  void open(File file);

  /**
   * The same, for a file the desk fetched rather than one picked off the disk: the address it
   * came from and which file inside it this turned out to be, so it can be kept and found again.
   */
  default void open(File file, String from, String entry) {
    open(file);
  }

  /** Nothing in it yet, so the next file can go here rather than in a second window. */
  boolean empty();

  /** Where what is in it came from, for keeping it; null when there is nothing to come back to. */
  default String cameFrom() {
    return null;
  }

  /** Which file inside that archive it was, when it came out of one. */
  default String entryInside() {
    return null;
  }

  /** What to call it in a list of favourites. */
  default String label() {
    return null;
  }
}

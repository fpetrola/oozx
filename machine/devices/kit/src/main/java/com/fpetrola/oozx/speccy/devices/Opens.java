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

  /** Nothing in it yet, so the next file can go here rather than in a second window. */
  boolean empty();
}

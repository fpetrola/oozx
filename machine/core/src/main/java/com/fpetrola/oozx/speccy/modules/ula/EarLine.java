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


package com.fpetrola.oozx.speccy.modules.ula;

/**
 * The tape socket, as the ULA reads it: something plugged in drives the line, or nothing does.
 * <p>
 * This way round because that is the way round the machine is wired. The ULA used to hold a
 * deck and ask it whether its ear bit was high, which made the deck a part of every Spectrum -
 * a machine could not be built without one, and a build that did not want a deck could not say
 * so. A Spectrum has a socket; what is in it is somebody else's business.
 * <p>
 * With nothing in it the line reads low, which is a real machine with no lead in the back.
 */
public interface EarLine {

  /** What the socket is carrying now. */
  boolean high();

  /**
   * Whether what is plugged in is playing, which is when the speaker keeps out of the way: the
   * loading noise is the tape's, not the beeper's.
   */
  default boolean playing() {
    return false;
  }

  /** Nothing in the socket: low, and quiet. */
  EarLine NOTHING_PLUGGED_IN = () -> false;
}

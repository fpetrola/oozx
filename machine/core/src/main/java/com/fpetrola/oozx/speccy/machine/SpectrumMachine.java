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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;

import java.util.Set;

public interface SpectrumMachine {
  int reset();

  void memoryMap();

  int unattachedPort(int port);

  Paging paging();

  /** The parts on the board, each as the class of the device it is: what is switched on without anybody asking. */
  default Set<Class<? extends Peripheral>> onBoard() {
    return Set.of();
  }

  default boolean hasOnBoard(Class<?> part) {
    return onBoard().stream().anyMatch(part::isAssignableFrom);
  }

  /** The numbers this model was measured to have. */
  MachineTimings getTimings();

  /** The T-state at which the beam starts that displayed line, the border's lines included. */
  long lineStart(int line);

  /** Told where its first displayed line starts, which is the one number every line is worked out from. */
  void firstLineAt(long tState);

  String getName();

  default void reset(boolean b) {
    reset();
  }

  boolean portFromUla(int port);

  /**
   * How many frames this machine has run, counting up and never back. A device that measures time
   * across frames needs it, because the t-state clock is rebased at every frame end.
   */
  long frameCount();

  /** Whether this machine decodes a port on all of its bits, where a Sinclair one looks at a few. */
  default boolean fullyDecodesPorts() {
    return false;
  }

  /**
   * Whether the ULA leaves the video data it is reading on the bus, which is what a port nothing
   * answers to reads back. The Amstrad machines and the Pentagon drive theirs instead.
   */
  default boolean hasFloatingBus() {
    return true;
  }

  /** Whether writing port 0x7ffd pages RAM and ROM the way the 128 does. */
  default boolean pagesThrough7ffd() {
    return false;
  }

  /** Whether writing port 0x1ffd pages the way the +3 does, on top of 0x7ffd. */
  default boolean pagesThrough1ffd() {
    return false;
  }

  /**
   * The short name a core speaks - what libretro and the test driver call this machine, as against
   * getName, which is what the box shows.
   */
  default String shortName() {
    return getName();
  }

  /**
   * Which machine a snapshot names when it was taken on this one, or null for a machine no
   * snapshot format can name - a variant then loads into the machine it is a variant of.
   */
  default MachineTypes snapshotModel() {
    return null;
  }

  /** What the ULA's own port reads on the bits the keyboard does not drive, after this write to it. */
  byte ulaPortIdleValue(byte lastOut);

  /** Whether this port gives the tape and the program a bit each, which a Timex does not. */
  default boolean separatesTapeFromSpeaker() {
    return true;
  }
}
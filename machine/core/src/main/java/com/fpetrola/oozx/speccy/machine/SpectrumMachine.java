/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
   * Whether the ULA leaves the video data it is reading on the bus, which is what a port nothing
   * answers to reads back. The Amstrad machines and the Pentagon drive theirs instead.
   */
  /**
   * How many frames this machine has run, counting up and never back. A device that measures time
   * across frames needs it, because the t-state clock is rebased at every frame end.
   */
  long frameCount();

  /** Whether this machine decodes a port on all of its bits, where a Sinclair one looks at a few. */
  default boolean fullyDecodesPorts() {
    return false;
  }

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
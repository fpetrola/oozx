/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
 */package com.fpetrola.oozx.speccy.modules.sound;

/**
 * Where the mix goes: the platform's audio, or nothing. The machine only ever hands it frames;
 * which card answers is the emulator's to bind, and without one the machine plays into silence.
 */
public interface SoundCard {
  /** Opens for that rate and channel count, either adjusted to what the card can do; 0 when open. */
  int open(String device, int[] freq, int[] stereo);

  void play(int[] samples, int count);

  void close();

  /**
   * Whether a frame the card has no room for is dropped rather than waited on. Waiting is what
   * holds the machine to real time; dropping is what lets it run ahead and still be heard.
   */
  default void dropWhenAhead(boolean drop) {
  }

  default boolean isOpen() {
    return false;
  }
}

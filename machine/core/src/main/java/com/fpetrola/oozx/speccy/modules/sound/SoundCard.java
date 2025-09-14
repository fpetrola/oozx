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

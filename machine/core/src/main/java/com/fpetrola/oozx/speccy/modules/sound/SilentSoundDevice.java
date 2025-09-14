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

/** The card when there is none: what a test hears, and what the machine plays into until a card is bound. */
public class SilentSoundDevice implements SoundCard {
  public int open(String device, int[] freq, int[] stereo) {
    return 0;
  }

  public void play(int[] samples, int count) {
  }

  public void close() {
  }
}

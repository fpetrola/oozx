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

package com.fpetrola.oozx.speccy.ports;

import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.SpectrumZ80Clock;

/**
 * The two ports of the AY-3-8912, which nothing was listening to.
 * <p>
 * The chip is addressed twice over: a write to 0xFFFD says which of its sixteen registers is
 * being spoken to, and a write to 0xBFFD sends a value to whichever that was. Neither port was
 * decoded anywhere, so a game's music went out to a chip that never heard a word of it - the
 * synthesis for it has been sitting in {@link Sound} the whole time, mixed into the output and
 * fed nothing. Sound effects came through because those are the beeper, on a different port
 * entirely, which is why a 128K game was half audible.
 * <p>
 * Decoded by address lines rather than by the whole number, which is how the machine itself does
 * it and how everything else in this package is written: bit 15 and bit 14 choose between the
 * two, and bit 1 must be low for the chip to answer at all.
 */
public class AyPortHandler extends DefaultPortHandler {

  /** Which register the next value is for; shared by the two ports, because the chip shares it. */
  private final int[] selected;
  private final boolean selects;
  private final Sound sound;
  private final SpectrumZ80Clock clock;

  public AyPortHandler(int mask, int value, boolean selects, int[] selected, Sound sound,
                       SpectrumZ80Clock clock) {
    super(mask, value, false, true);
    this.selects = selects;
    this.selected = selected;
    this.sound = sound;
    this.clock = clock;
  }

  @Override
  public void write(int port, byte value) {
    if (selects) {
      selected[0] = value & 0x0F;
    } else {
      // With the time it happened, because a tune is the changes AND when each one was made:
      // handed over without that, every note of a frame would begin at once.
      sound.ayWrite(selected[0], value & 0xFF, clock.getTStates());
    }
  }
}

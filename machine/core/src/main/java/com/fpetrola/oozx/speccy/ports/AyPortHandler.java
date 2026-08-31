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
import com.fpetrola.z80.cpu.Z80Clock;

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

  private final AyRegisters registers;
  private final boolean selects;
  private final Sound sound;
  private final Z80Clock clock;

  public AyPortHandler(int mask, int value, boolean selects, AyRegisters registers, Sound sound,
                       Z80Clock clock) {
    this(mask, value, selects, registers, sound, clock, false);
  }

  /**
   * @param alsoAnswers whether this port reads as well as writes. The register port always does -
   *                    a program that writes a value and reads it back is asking whether there is
   *                    a chip here, and silence is a "no". On a +3 the data port answers too.
   */
  public AyPortHandler(int mask, int value, boolean selects, AyRegisters registers, Sound sound,
                       Z80Clock clock, boolean alsoAnswers) {
    super(mask, value, selects || alsoAnswers, true);
    this.selects = selects;
    this.registers = registers;
    this.sound = sound;
    this.clock = clock;
  }

  @Override
  public void write(int port, byte value) {
    if (selects) {
      registers.select(value);
    } else {
      registers.write(value);
      // With the time it happened, because a tune is the changes AND when each one was made:
      // handed over without that, every note of a frame would begin at once.
      sound.ayWrite(registers.current(), value & 0xFF, clock.getTStates());
    }
  }

  @Override
  public byte read(int port, byte[] attached) {
    attached[0] = (byte) 0xff;
    return (byte) registers.read();
  }
}

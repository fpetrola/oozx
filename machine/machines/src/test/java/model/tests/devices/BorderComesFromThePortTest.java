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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The three lowest bits of the ULA's port are the border, and it is the ULA that hears them.
 * <p>
 * That a border change lands where the beam is, is the picture's business and is asked there;
 * what is asked here is the wire: a program writes 0xFE and the border becomes what it wrote.
 * Nothing here asked it - the picture's tests drive the
 * border by calling it, which is the one path a broken port would not break.
 */
class BorderComesFromThePortTest {

  private final Speccy speccy = speccy();

  private static Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    return speccy;
  }

  /** The border as painted, taken from a pixel that is border on every machine. */
  private int borderColour() {
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
    int rgb = speccy.picture.pixels[5 * Picture.WIDTH + 8];
    for (int colour = 0; colour < Picture.PALETTE.length; colour++) {
      if (Picture.PALETTE[colour] == rgb) return colour;
    }
    return -1;
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
  void everyColourWrittenToThePortIsTheBorder(int colour) {
    speccy.ports.write(0x00FE, (byte) colour);
    assertEquals(colour, borderColour());
  }

  /** Bit 3 is the tape and bit 4 the speaker: what a program writes there is not a colour. */
  @Test
  void andNothingAboveTheThirdBitIs() {
    speccy.ports.write(0x00FE, (byte) 0xFA);
    assertEquals(2, borderColour(), "0xFA is red with the speaker and the tape bits set, and red is all of it");
  }

  /** A ULA answers every even port, so the border does too - it is not the port number 0x00FE. */
  @Test
  void andSoIsEveryOtherEvenPort() {
    speccy.ports.write(0x7FFE, (byte) 4);
    assertEquals(4, borderColour());
    speccy.ports.write(0x1234, (byte) 6);
    assertEquals(6, borderColour());
  }
}

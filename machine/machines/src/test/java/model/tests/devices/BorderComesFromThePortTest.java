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
 * Confirms the port wiring itself: bits 0-2 written to the ULA port become the border colour.
 * Beam-position timing of the change is the picture module's own concern, tested there; this
 * covers only the path a broken port decode would actually break.
 */
class BorderComesFromThePortTest {

  private final Speccy speccy = speccy();

  private static Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    return speccy;
  }

  /** Reads back the rendered border colour from a pixel that is always border, on any model. */
  private int borderColour() {
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
    int rgb = speccy.picture.pixels[5 * Picture.STRIDE + 8];
    for (int colour = 0; colour < Picture.SINCLAIR.length; colour++) {
      if (Picture.SINCLAIR[colour] == rgb) return colour;
    }
    return -1;
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
  void everyColourWrittenToThePortIsTheBorder(int colour) {
    speccy.ports.write(0x00FE, (byte) colour);
    assertEquals(colour, borderColour());
  }

  /** Bits 3-4 (tape/speaker) must not be mistaken for part of the border colour. */
  @Test
  void andNothingAboveTheThirdBitIs() {
    speccy.ports.write(0x00FE, (byte) 0xFA);
    assertEquals(2, borderColour(), "0xFA is red with the speaker and the tape bits set, and red is all of it");
  }

  /** The ULA decodes any even port address, not specifically 0x00FE, so border writes work on any. */
  @Test
  void andSoIsEveryOtherEvenPort() {
    speccy.ports.write(0x7FFE, (byte) 4);
    assertEquals(4, borderColour());
    speccy.ports.write(0x1234, (byte) 6);
    assertEquals(6, borderColour());
  }
}

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

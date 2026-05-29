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
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;
import com.fpetrola.oozx.speccy.devices.melodik.MelodikPeripheral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Melodik: an external AY box for the 48K, decoded identically to a 128's built-in chip, so
 * 128K-targeted music plays unmodified. First device here that is neither a machine feature nor
 * built into one - presence depends on what is plugged in, not on what the machine reports.
 */
class MelodikTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void play(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private Speccy aFortyEightWith(boolean melodik) {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).toInstance(new Loudest()));
    speccy.init();
    speccy.picture.active = false;
    ((MelodikPeripheral) speccy.peripheralRegistry.find(MelodikPeripheral.class)).setFitted(melodik);
    speccy.sound.output.enabled = true;
    speccy.machine.selectDefault();
    return speccy;
  }

  @Test
  void aFortyEightWithOneHasASoundChip() {
    Speccy speccy = aFortyEightWith(true);
    assertTrue(speccy.peripheralRegistry.isActive(MelodikPeripheral.class), "the box was asked for and is not there");

    // Melodik answers on the same ports a 128's built-in AY would.
    speccy.ports.write(0xFFFD, (byte) 0);
    speccy.ports.write(0xBFFD, (byte) 0x50);
    speccy.ports.write(0xFFFD, (byte) 8);
    speccy.ports.write(0xBFFD, (byte) 0x0F);
    speccy.ports.write(0xFFFD, (byte) 7);
    speccy.ports.write(0xBFFD, (byte) 0x3E);
    speccy.sound.frame();

    Loudest heard = (Loudest) speccy.sound.card();
    assertTrue(heard.peak > 0, "a 48K with a Melodik in it made no sound");
  }

  /** Regression: a peripheral was only notified on activation, not deactivation, leaving its
   * chip stuck in the mixer producing silence after its ports were removed. */
  @Test
  void andPullingItOutTakesTheChipWithIt() {
    Speccy speccy = aFortyEightWith(true);
    Loudest heard = (Loudest) speccy.sound.card();

    speccy.ports.write(0xFFFD, (byte) 8);
    speccy.ports.write(0xBFFD, (byte) 0x0F);
    speccy.ports.write(0xFFFD, (byte) 7);
    speccy.ports.write(0xBFFD, (byte) 0x3E);
    speccy.ports.write(0xFFFD, (byte) 0);
    speccy.ports.write(0xBFFD, (byte) 0x50);
    speccy.sound.frame();
    assertTrue(heard.peak > 0, "it was not playing before being unplugged");

    ((MelodikPeripheral) speccy.peripheralRegistry.find(MelodikPeripheral.class)).setFitted(false);
    speccy.peripheralRegistry.update();
    assertFalse(speccy.peripheralRegistry.isActive(MelodikPeripheral.class), "it is still plugged in");

    heard.peak = 0;
    speccy.sound.frame();
    assertEquals(0, heard.peak, "the box was pulled out and its chip is still playing");
  }

  /** With no Melodik fitted, its ports must answer nothing on a 48K. */
  @Test
  void andWithoutOneItIsStillSilent() {
    Speccy speccy = aFortyEightWith(false);
    assertFalse(speccy.peripheralRegistry.isActive(MelodikPeripheral.class));

    speccy.ports.write(0xFFFD, (byte) 8);
    speccy.ports.write(0xBFFD, (byte) 0x0F);
    speccy.ports.write(0xFFFD, (byte) 7);
    speccy.ports.write(0xBFFD, (byte) 0x3E);
    speccy.sound.frame();

    assertEquals(0, ((Loudest) speccy.sound.card()).peak,
        "a plain 48K made sound chip music, which a plain 48K cannot");
  }
}

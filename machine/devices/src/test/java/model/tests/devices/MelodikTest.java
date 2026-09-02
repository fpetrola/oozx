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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;
import com.fpetrola.oozx.speccy.devices.ay.MelodikPeripheral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A 48K with a box of sound chip plugged into it.
 * <p>
 * The Melodik decodes the chip exactly as a 128 does, so music written for a 128K plays on a 48K
 * with one in the back. It is the first thing here that is neither part of a machine nor built
 * into one - and the case that says presence is about what is plugged in rather than about what
 * the machine is, since the machine says it has no sound chip and there is one.
 */
class MelodikTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private Speccy aFortyEightWith(boolean melodik) {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(new Loudest()));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.melodik = melodik;
    speccy.settings.current.sound = true;
    speccy.machine.selectDefault();
    return speccy;
  }

  @Test
  void aFortyEightWithOneHasASoundChip() {
    Speccy speccy = aFortyEightWith(true);
    assertTrue(speccy.peripherals.isActive(MelodikPeripheral.class), "the box was asked for and is not there");

    // The 128's own ports, which is what the box answers on.
    speccy.peripherals.writePort(0xFFFD, (byte) 0);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x50);
    speccy.peripherals.writePort(0xFFFD, (byte) 8);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x0F);
    speccy.peripherals.writePort(0xFFFD, (byte) 7);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x3E);
    speccy.sound.frame();

    Loudest heard = (Loudest) speccy.sound.getJavaSoundDevice();
    assertTrue(heard.peak > 0, "a 48K with a Melodik in it made no sound");
  }

  /**
   * And pulled out again, which has to be as complete as putting it in.
   * <p>
   * A peripheral was told when it was switched on and never when it was switched off, so its ports
   * went and its chip stayed - sitting in the mixer making silence out of a queue nothing was
   * filling any more.
   */
  @Test
  void andPullingItOutTakesTheChipWithIt() {
    Speccy speccy = aFortyEightWith(true);
    Loudest heard = (Loudest) speccy.sound.getJavaSoundDevice();

    speccy.peripherals.writePort(0xFFFD, (byte) 8);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x0F);
    speccy.peripherals.writePort(0xFFFD, (byte) 7);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x3E);
    speccy.peripherals.writePort(0xFFFD, (byte) 0);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x50);
    speccy.sound.frame();
    assertTrue(heard.peak > 0, "it was not playing before being unplugged");

    speccy.settings.current.melodik = false;
    speccy.peripherals.update();
    assertFalse(speccy.peripherals.isActive(MelodikPeripheral.class), "it is still plugged in");

    heard.peak = 0;
    speccy.sound.frame();
    assertEquals(0, heard.peak, "the box was pulled out and its chip is still playing");
  }

  /** And a 48K without one is still a 48K: no chip, and its ports answer nothing. */
  @Test
  void andWithoutOneItIsStillSilent() {
    Speccy speccy = aFortyEightWith(false);
    assertFalse(speccy.peripherals.isActive(MelodikPeripheral.class));

    speccy.peripherals.writePort(0xFFFD, (byte) 8);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x0F);
    speccy.peripherals.writePort(0xFFFD, (byte) 7);
    speccy.peripherals.writePort(0xBFFD, (byte) 0x3E);
    speccy.sound.frame();

    assertEquals(0, ((Loudest) speccy.sound.getJavaSoundDevice()).peak,
        "a plain 48K made sound chip music, which a plain 48K cannot");
  }
}

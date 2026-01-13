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
 */package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** With this module on the classpath the mix reaches the platform's audio; a test can still ask for silence over it. */
class TheCardComesWithTheModuleTest {
  @Test
  void theModuleBindsThePlatformsCard() {
    assertInstanceOf(JavaSoundDevice.class, Speccy.create().sound.card());
  }

  @Test
  void aTestCanStillPlayIntoSilence() {
    Speccy speccy = Speccy.create(new com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    assertInstanceOf(SilentSoundDevice.class, speccy.sound.card());
  }
}

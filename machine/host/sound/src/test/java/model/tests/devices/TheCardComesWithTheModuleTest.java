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

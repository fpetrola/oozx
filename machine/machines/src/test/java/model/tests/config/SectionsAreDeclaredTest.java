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
package model.tests.config;

import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression: an undeclared config section fails silently, since the DI container just builds a
 * fresh instance at every injection point, so a settings change is invisibly ignored - as
 * happened when the input section went undeclared and a requested Kempston never appeared.
 */
class SectionsAreDeclaredTest {
  @Test
  void everySectionAMachineUsesComesFromItsConfiguration() {
    Injector injector = Guice.createInjector(Modules.override(new com.fpetrola.oozx.EmulatorModule(new SpectrumZ80Clock()))
        .with(binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class)));
    injector.getInstance(Speccy.class);
    Configuration configuration = injector.getInstance(Configuration.class);

    // Confirms the config file's section instance matches what's actually injected elsewhere.
    for (Class<?> section : List.of(com.fpetrola.oozx.config.RomFiles.class))
      assertSame(configuration.of(section), injector.getInstance(section),
          section.getSimpleName() + " is not declared with Configuration.section, so everything that asks for it gets its own");

    // Must each be a true singleton, or the timer and sound could read different, stale values.
    for (Class<?> value : List.of(Speed.class, Rom.Protection.class, Machine.Unit.class, Sound.Output.class))
      assertSame(injector.getInstance(value), injector.getInstance(value),
          value.getName() + " is not a singleton, so everything that asks for it gets its own");
  }
}

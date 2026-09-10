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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Section;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Modules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The whole configuration of a machine with everything plugged in, in one file, so it can be read.
 * <p>
 * Written from the classes rather than beside them: the defaults are the field initialisers and
 * nothing else says what they are, so this is a copy that cannot disagree with them - it is
 * produced by asking a real machine for every section it has and saving it. It is not read back;
 * what an emulator runs on is what its classes say and what somebody changed.
 * <p>
 * Rewrite it with {@code mvn test -pl machine/app -Doozx.generate=true -Dtest=ConfigurationDocumentTest}.
 */
/**
 * doc/configuracion.json shows every setting this build has with what ships as its value. It is
 * written from the classes and checked against them here, so that it cannot drift; it did, for
 * as long as this class was named so that surefire never ran it.
 */
class ConfigurationDocumentTest {
  static final Path REFERENCE = Path.of("../../doc/configuracion.json");

  /** Every section this build has, asked for from a machine that has every peripheral. */
  static String everything() throws Exception {
    Injector injector = Guice.createInjector(Modules.override(new com.fpetrola.oozx.EmulatorModule(new SpectrumZ80Clock()))
        .with(binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class)));
    injector.getInstance(Speccy.class);

    File file = File.createTempFile("configuration", ".json");
    file.deleteOnExit();
    Configuration configuration = new Configuration(file);
    injector.getAllBindings().keySet().stream()
        .map(key -> key.getTypeLiteral().getRawType())
        .filter(type -> type.isAnnotationPresent(Section.class))
        .sorted(java.util.Comparator.comparing(type -> type.getAnnotation(Section.class).value()))
        .forEach(configuration::of);
    configuration.save();
    return Files.readString(file.toPath());
  }

  @Test
  @EnabledIfSystemProperty(named = "oozx.generate", matches = "true")
  void write() throws Exception {
    Files.writeString(REFERENCE, everything());
    System.out.println("written " + REFERENCE.toAbsolutePath().normalize());
  }

  @Test
  void theFileThatShowsTheConfigurationIsWhatTheClassesSay() throws Exception {
    assertEquals(Files.readString(REFERENCE), everything(),
        "doc/configuracion.json is stale: run mvn test -pl machine/app -Doozx.generate=true -Dtest=ConfigurationDocumentTest");
  }
}

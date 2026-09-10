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

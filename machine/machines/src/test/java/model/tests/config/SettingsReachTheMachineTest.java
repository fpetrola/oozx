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
import com.fpetrola.oozx.config.Configuration;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The file's values reach the machine's parts from outside, and the parts' values reach the file
 * on save, with no part of the machine knowing there is a file.
 */
class SettingsReachTheMachineTest {
  private static Speccy machineWith(File file) {
    return MachineTest.silentMachine(binder -> binder.bind(Configuration.class).toInstance(new Configuration(file)));
  }

  @Test
  void whatTheFileSaysGoesIntoTheMachine() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{\"speed\": {\"emulation\": 123, \"fastLoading\": true}}");
    Speccy speccy = machineWith(file.toFile());
    assertEquals(123, speccy.speed.emulation);
    assertTrue(speccy.speed.fastLoading);
  }

  @Test
  void whatTheFileDoesNotSayIsWhatShipped() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{}");
    Speccy speccy = machineWith(file.toFile());
    assertEquals(Configuration.shipped().valueOf("speed", "emulation", int.class), speccy.speed.emulation, "the default is what shipped on the classpath, not the file's");
  }

  @Test
  void whatTheMachineHoldsIsWhatTheFileWrites() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{}");
    Speccy speccy = machineWith(file.toFile());
    speccy.speed.emulation = 456;
    speccy.configuration.save();
    assertEquals(456, machineWith(file.toFile()).speed.emulation, "the speed the knob was turned to is the one read back");
  }

  @Test
  void aMachineThatEndedNoLongerWritesItsValues() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{\"speed\": {\"emulation\": 123}}");
    Configuration shared = new Configuration(file.toFile());
    Speccy gone = MachineTest.silentMachine(binder -> binder.bind(Configuration.class).toInstance(shared));
    gone.speed.emulation = 456;
    gone.end();
    shared.save();
    assertEquals(123, machineWith(file.toFile()).speed.emulation, "what a closed machine held stays out of the file");
  }
}

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

import dev.plugdesk.app.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the one configuration file promises: the defaults live in the classes, the file carries
 * only what someone changed, and a section this build does not know is kept rather than lost.
 */
class ConfigurationTest {
  /** The ROM table is the file's: an entry it names goes over what shipped, and the rest stay. */
  @Test
  void aRomTheFileNamesGoesOverTheOneThatShippedAndTheRestStay(@TempDir Path dir) throws Exception {
    com.fpetrola.oozx.config.RomFiles roms = new Configuration(write(dir, """
        { "roms": { "files": { "DivIdePeripheral": ["/mine/fatware.rom"] } } }""")).of(com.fpetrola.oozx.config.RomFiles.class);
    assertEquals(List.of("/mine/fatware.rom"), roms.files.get("DivIdePeripheral"), "one nothing shipped with");
    // What this build carries is the 48K and its ROM: every other machine and every board says
    // what it is made with in its own jar, so this is the whole of what a file can leave alone.
    assertEquals(List.of("48.rom"), roms.files.get("Spec48"), "the one the file did not name");
  }

  private static File write(Path dir, String json) throws Exception {
    File file = dir.resolve("config.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }
}
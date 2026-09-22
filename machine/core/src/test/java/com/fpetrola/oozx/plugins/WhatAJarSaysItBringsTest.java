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

package com.fpetrola.oozx.plugins;

import com.fpetrola.oozx.Extension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a jar brought is read from what it says it brings, without building any of it. */
class WhatAJarSaysItBringsTest {

  @TempDir
  Path folder;

  private File jarSaying(String... services) throws IOException {
    Path jar = folder.resolve("something.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      for (String said : services) {
        String wayIn = said.substring(0, said.indexOf('='));
        out.putNextEntry(new JarEntry("META-INF/services/" + wayIn));
        out.write(said.substring(said.indexOf('=') + 1).replace(',', '\n').getBytes());
        out.closeEntry();
      }
    }
    return jar.toFile();
  }

  @Test
  void itSaysWhichWayInEachClassAnswersTo() throws IOException {
    File jar = jarSaying(Extension.class.getName() + "=com.probe.Devices,com.probe.More",
        BesideTheGame.class.getName() + "=com.probe.Colours");

    List<Plugins.WhatIsIn> inside = Plugins.whatIsIn(jar);

    assertEquals(3, inside.size(), "everything the jar says it brings: " + inside);
    assertTrue(inside.stream().allMatch(one -> one.from().equals("something.jar")));
    assertEquals(List.of("com.probe.Colours", "com.probe.Devices", "com.probe.More"),
        inside.stream().map(Plugins.WhatIsIn::implementation).sorted().toList());
    assertEquals("extra", inside.stream()
        .filter(one -> one.implementation().equals("com.probe.Colours"))
        .findFirst().orElseThrow().kind(), "the kind comes from the way in itself");
  }

  /** A jar may serve anything at all through META-INF/services; only a way in is a plugin. */
  @Test
  void whatIsNotAWayInIsNotSomethingThatWasPluggedIn() throws IOException {
    File jar = jarSaying("java.sql.Driver=com.probe.Driver",
        Extension.class.getName() + "=com.probe.Devices");

    List<Plugins.WhatIsIn> inside = Plugins.whatIsIn(jar);

    assertEquals(List.of("com.probe.Devices"),
        inside.stream().map(Plugins.WhatIsIn::implementation).toList());
  }

  /** Nothing is loaded to find out: the classes named here do not exist anywhere. */
  @Test
  void nothingItNamesIsBuiltToFindOut() throws IOException {
    File jar = jarSaying(Extension.class.getName() + "=com.probe.NotAClassAnywhere");

    assertEquals(1, Plugins.whatIsIn(jar).size());
  }
}

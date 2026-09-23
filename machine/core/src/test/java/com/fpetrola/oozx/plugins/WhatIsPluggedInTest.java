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

import com.fpetrola.oozx.plugins.PluginReleases.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What this emulator has plugged in is a fact about the folder.
 * <p>
 * It used to be decided by comparing the asset a jar came from against the asset published now,
 * and the build recreates every release on every push, so one push made every installed board
 * disappear from the list while the machines went on using all of them.
 */
class WhatIsPluggedInTest {

  private String previousHome;
  private String previousPlugins;

  @BeforeEach
  void aHomeOfItsOwn(@TempDir Path home) throws IOException {
    previousHome = System.getProperty("user.home");
    previousPlugins = System.getProperty("oozx.plugins");
    System.setProperty("user.home", home.toString());
    // The tests run with plugins off so that what they build is this tree; this one is about
    // the folder, so for as long as it runs the folder is looked at.
    System.clearProperty("oozx.plugins");
    Files.createDirectories(Plugins.folder());
  }

  @AfterEach
  void putItBack() {
    System.setProperty("user.home", previousHome);
    if (previousPlugins == null) {
      System.clearProperty("oozx.plugins");
    } else {
      System.setProperty("oozx.plugins", previousPlugins);
    }
  }

  private static void lying(String jar) throws IOException {
    Files.writeString(Plugins.folder().resolve(jar), "not really a jar");
  }

  private static Board published(String jar, long asset) {
    return new Board("Calls", jar, "https://example.invalid/" + jar, asset, 1024);
  }

  /**
   * The regression: the copy here came from one asset and the release was recreated with
   * another, which says nothing about whether the board is here. It is here.
   */
  @Test
  void aReleaseBuiltAgainDoesNotUnplugWhatIsInstalled() throws IOException {
    lying("tool-calls-0.0.2-alu-SNAPSHOT.jar");

    assertTrue(PluginReleases.isHere(published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 999)),
        "the jar is in the folder, whatever asset the release carries today");
  }

  @Test
  void whatIsNotInTheFolderIsNotHere() {
    assertFalse(PluginReleases.isHere(published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 1)));
  }

  /** The same board at another version is the same board, which is what the file name hides. */
  @Test
  void aBoardIsTheSameBoardAtAnotherVersion() throws IOException {
    lying("tool-calls-0.0.3.jar");

    assertTrue(PluginReleases.isHere(published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 1)),
        "the version moved, the board did not");
    assertEquals("tool-calls", PluginReleases.whichBoard("tool-calls-0.0.2-alu-SNAPSHOT.jar"));
    assertEquals("tool-calls", PluginReleases.whichBoard("tool-calls-0.0.3.jar"));
    assertEquals("device-mouse", PluginReleases.whichBoard("device-mouse.jar"));
  }

  /**
   * Estar en la carpeta no es estar enchufado: lo enchufado es lo que llego a cargarse, que es
   * lo que la ventana tiene que decir. Un archivo que no es un plugin no lo es por estar ahi.
   */
  @Test
  void aFileThatIsNotAPluginIsNotPluggedIn() throws IOException {
    lying("tool-calls-0.0.2-alu-SNAPSHOT.jar");
    lying("tool-something-of-my-own-0.1.jar");

    assertEquals(List.of(), PluginReleases.here(
            List.of(published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 7))),
        "ninguno de los dos es un plugin, por mas que el nombre lo parezca");
  }

  @Test
  void aNewerOneBeingPublishedIsSaidSeparatelyFromBeingHere() throws IOException {
    lying("tool-calls-0.0.2-alu-SNAPSHOT.jar");
    Board republished = published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 580074263L);

    assertTrue(PluginReleases.isHere(republished), "it is here");
    assertTrue(PluginReleases.isNewerThanHere(republished),
        "and what is published came from another build");
  }
}

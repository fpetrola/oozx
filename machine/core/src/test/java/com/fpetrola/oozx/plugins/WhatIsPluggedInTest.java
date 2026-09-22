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

  /** A jar built here or copied in by hand is as plugged in as one that was downloaded. */
  @Test
  void whatNobodyPublishedIsStillPluggedIn() throws IOException {
    lying("tool-calls-0.0.2-alu-SNAPSHOT.jar");
    lying("tool-something-of-my-own-0.1.jar");

    List<Board> here = PluginReleases.here(
        List.of(published("tool-calls-0.0.2-alu-SNAPSHOT.jar", 7)));

    assertEquals(2, here.size(), "both are in the folder, so both are in");
    assertEquals("Calls", here.get(0).name(), "the published one keeps its release's name");
    assertEquals("tool-something-of-my-own", here.get(1).name(),
        "and one nobody published is called after its file");
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

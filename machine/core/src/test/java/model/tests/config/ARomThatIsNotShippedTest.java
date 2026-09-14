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

import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some machines need a ROM this project cannot ship. Rather than leave them out, the file says
 * where that ROM is published and what it should turn out to be, and the emulator fetches it once
 * if the person in front of it agrees - so the bytes come from whoever publishes them, not from us.
 * <p>
 * Nothing here reaches the network: a file is published on this disk, which is what a URL is for.
 */
class ARomThatIsNotShippedTest {
  /** Long enough to be asked for as a ROM, short enough to write in a test. */
  private static final int LENGTH = 64;
  private static final String NAME = "not-shipped.rom";

  private String previousHome;
  private RomFiles roms;
  private int timesAsked;
  private String publishedAt;

  /** A device with a ROM of its own, which is the smallest thing that can ask for one. */
  private static class Asking extends AbstractPeripheral {
    Asking() {
      super(List.of());
    }
  }

  private final Asking asking = new Asking();

  @BeforeEach
  void aHomeOfItsOwn(@TempDir Path home) throws Exception {
    previousHome = System.getProperty("user.home");
    System.setProperty("user.home", home.toString());
    byte[] published = new byte[LENGTH];
    for (int at = 0; at < LENGTH; at++) published[at] = (byte) (at * 7);
    File file = home.resolve("published.rom").toFile();
    Files.write(file.toPath(), published);
    publishedAt = file.toURI().toString();
    roms = new RomFiles();
    roms.files.put("Asking", List.of(NAME));
    timesAsked = 0;
    RomFiles.askingFirst((rom, from) -> {
      timesAsked++;
      return true;
    });
  }

  @AfterEach
  void backToWhereverHomeWas() {
    System.setProperty("user.home", previousHome);
    RomFiles.askingFirst(null);
  }

  private void publishedAs(String sha256) {
    RomFiles.Source source = new RomFiles.Source();
    source.url = publishedAt;
    source.sha256 = sha256;
    roms.sources.put(NAME, source);
  }

  private String whatArrives() {
    try {
      roms.of(asking, LENGTH);
      return null;
    } catch (RomNotLoadedException itDidNotArrive) {
      return itDidNotArrive.getMessage();
    }
  }

  @Test
  void itIsFetchedOnceAndThenItIsKept() throws Exception {
    publishedAs(shaOfTheFile());
    byte[] first = roms.of(asking, LENGTH);

    assertEquals(1, timesAsked, "it asked before going to fetch it");
    assertTrue(RomFiles.kept(NAME).isFile(), "the one that arrived was kept");
    assertArrayEquals(first, Files.readAllBytes(RomFiles.kept(NAME).toPath()), "and kept as it arrived");

    assertArrayEquals(first, roms.of(asking, LENGTH), "the same ROM the second time");
    assertEquals(1, timesAsked, "and it did not ask again, because it did not fetch again");
  }

  /** A ROM is the one thing a machine cannot work around being wrong about, so wrong bytes are no ROM at all. */
  @Test
  void bytesThatAreNotTheOnesAskedForAreRefused() {
    publishedAs("0".repeat(64));

    assertTrue(whatArrives().contains("is not the one expected"), "it should say the bytes are not the ones asked for");
    assertFalse(RomFiles.kept(NAME).isFile(), "and nothing that was refused was kept");
  }

  /** Saying where it comes from is not enough: something has to say what it is. */
  @Test
  void aSourceWithoutAnythingToCheckAgainstIsRefused() {
    publishedAs(null);

    assertTrue(whatArrives().contains("but not what it should be"));
  }

  @Test
  void whatIsNotAgreedToIsNotFetched() throws Exception {
    publishedAs(shaOfTheFile());
    RomFiles.askingFirst((rom, from) -> false);

    assertTrue(whatArrives().contains("couldn't find"), "without a yes there is nothing to find");
    assertFalse(RomFiles.kept(NAME).isFile());
  }

  /** A ROM that is in the build is answered from the build, so it can never reach out for one. */
  @Test
  void aRomThisBuildShipsIsNeverFetched() {
    roms.files.put("Asking", List.of("48.rom"));
    RomFiles.Source wrong = new RomFiles.Source();
    wrong.url = publishedAt;
    wrong.sha256 = "0".repeat(64);
    roms.sources.put("48.rom", wrong);

    assertEquals(0x4000, roms.of(asking, 0x4000).length, "the packaged one");
    assertEquals(0, timesAsked, "nothing was asked, because nothing was fetched");
  }

  private String shaOfTheFile() throws Exception {
    byte[] published = Files.readAllBytes(new File(java.net.URI.create(publishedAt)).toPath());
    StringBuilder digest = new StringBuilder();
    for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(published)) digest.append(String.format("%02x", b));
    return digest.toString();
  }
}

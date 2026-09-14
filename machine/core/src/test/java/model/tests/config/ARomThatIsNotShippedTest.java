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
      roms.bring(NAME);
      return null;
    } catch (RomNotLoadedException itDidNotArrive) {
      return itDidNotArrive.getMessage();
    }
  }

  @Test
  void itIsFetchedOnceAndThenItIsKept() throws Exception {
    publishedAs(shaOfTheFile());
    assertEquals(List.of(NAME), roms.missingFor(asking), "it is not here yet, and the machine says which one");
    assertTrue(roms.bring(NAME), "and it is here now");
    byte[] first = roms.of(asking, LENGTH);

    assertEquals(1, timesAsked, "it asked before going to fetch it");
    assertEquals(List.of(), roms.missingFor(asking), "and nothing is missing any more");
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

    assertFalse(roms.bring(NAME), "without a yes it does not arrive");
    assertFalse(RomFiles.kept(NAME).isFile());
  }

  /**
   * A machine is built on the emulator's own thread, and nothing there may wait on an answer from
   * somebody at a keyboard or from somewhere far away. Asking a machine for a ROM that is not here
   * fails at once; bringing it is a separate act, done by whoever is about to start the machine.
   */
  @Test
  void buildingAMachineNeverGoesLookingForARom() throws Exception {
    publishedAs(shaOfTheFile());

    RomNotLoadedException itIsNotHere = assertThrows(RomNotLoadedException.class, () -> roms.of(asking, LENGTH));
    assertEquals(NAME, itIsNotHere.file(), "and it says which one, so somebody can go and get it");
    assertEquals(0, timesAsked, "a machine being built stopped to ask a question");
  }

  /** A ROM that is in the build is answered from the build, so it can never reach out for one. */
  @Test
  void aRomThisBuildShipsIsNeverFetched() {
    roms.files.put("Asking", List.of("48.rom"));
    RomFiles.Source wrong = new RomFiles.Source();
    wrong.url = publishedAt;
    wrong.sha256 = "0".repeat(64);
    roms.sources.put("48.rom", wrong);

    assertEquals(List.of(), roms.missingFor(asking), "what the build carries was never missing");
    assertEquals(0x4000, roms.of(asking, 0x4000).length, "the packaged one");
    assertEquals(0, timesAsked, "nothing was asked, because nothing was fetched");
  }

  /**
   * How much has arrived, told as it arrives. A ROM is small, but the person who said yes is
   * waiting on a machine that will not start until it is here, and a wait with nothing moving is
   * the same as a wait that has gone wrong.
   */
  @Test
  void howMuchOfItHasArrivedIsToldWhileItArrives() throws Exception {
    byte[] big = new byte[64 * 1024];
    for (int at = 0; at < big.length; at++) big[at] = (byte) at;
    java.util.List<Long> told = new java.util.ArrayList<>();
    boolean[] finished = {false};
    long[] lengthSaid = {0};
    RomFiles.askingFirst(new RomFiles.Consent() {
      public boolean toDownload(String rom, String from) {
        return true;
      }

      public void arriving(String rom, long soFar, long length) {
        told.add(soFar);
        lengthSaid[0] = length;
      }

      public void arrived(String rom) {
        finished[0] = true;
      }
    });

    com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/rom", exchange -> {
      exchange.sendResponseHeaders(200, big.length);
      try (java.io.OutputStream out = exchange.getResponseBody()) {
        out.write(big);
      }
    });
    server.start();
    try {
      RomFiles.Source source = new RomFiles.Source();
      source.url = "http://127.0.0.1:" + server.getAddress().getPort() + "/rom";
      source.sha256 = shaOf(big);
      roms.sources.put(NAME, source);

      assertTrue(roms.bring(NAME));
      assertArrayEquals(big, roms.of(asking, big.length));
    } finally {
      server.stop(0);
    }

    assertTrue(told.size() > 1, "it arrived in one piece and nobody could have watched it: " + told);
    assertEquals(big.length, told.get(told.size() - 1), "the last thing said was that all of it was here");
    assertEquals(big.length, lengthSaid[0], "and it said how much there was to wait for");
    assertTrue(finished[0], "and that nothing more was coming");
  }

  private String shaOfTheFile() throws Exception {
    return shaOf(Files.readAllBytes(new File(java.net.URI.create(publishedAt)).toPath()));
  }

  private static String shaOf(byte[] bytes) throws Exception {
    StringBuilder digest = new StringBuilder();
    for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(bytes)) digest.append(String.format("%02x", b));
    return digest.toString();
  }
}

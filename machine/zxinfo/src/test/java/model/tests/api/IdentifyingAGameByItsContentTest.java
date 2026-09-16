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
package model.tests.api;

import com.fpetrola.oozx.api.GameFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the fingerprint claims is that a copy is still recognisable after the things that are
 * actually done to Spectrum images: a trainer in front, a container that interleaves its own
 * headers, a handful of pokes. Each test breaks the file the way one of those does.
 */
class IdentifyingAGameByItsContentTest {
  private static final Path MANIC_MINER = Path.of("../../doc/manicminer.z80");

  private final GameFingerprint.Index index = new GameFingerprint.Index();
  private byte[] game;

  @BeforeEach
  void fillTheCatalogue() throws IOException {
    game = Files.readAllBytes(MANIC_MINER);
    index.add(GameFingerprint.named("0003012", "manicminer"), GameFingerprint.of(game));
    for (int other = 0; other < 20; other++) {
      index.add(GameFingerprint.named("other" + other, "other" + other), GameFingerprint.of(noise(other, game.length)));
    }
  }

  @Test
  void theSameFileIsTheSameGame() {
    assertEquals("manicminer", index.identify(game).game().title);
    assertEquals(1.0, index.identify(game).score());
  }

  @Test
  void aTrainerInFrontDoesNotMoveTheGameOutOfReach() {
    GameFingerprint.Match match = index.identify(concat(noise(99, 3000), game));
    assertEquals("manicminer", match.game().title);
    assertTrue(match.score() > 0.9, "score: " + match.score());
  }

  @Test
  void aContainerThatInterleavesItsOwnHeadersLosesOnlyTheChunksItTouches() {
    byte[] withHeaders = new byte[0];
    for (int at = 0; at < game.length; at += 2048) {
      withHeaders = concat(concat(withHeaders, noise(at, 8)),
          Arrays.copyOfRange(game, at, Math.min(at + 2048, game.length)));
    }
    GameFingerprint.Match match = index.identify(withHeaders);
    assertEquals("manicminer", match.game().title);
    assertTrue(match.score() > 0.7, "score: " + match.score());
  }

  @Test
  void pokesDamageOneChunkEach() {
    byte[] poked = game.clone();
    Random where = new Random(7);
    for (int poke = 0; poke < 10; poke++) {
      poked[where.nextInt(poked.length)] = (byte) 0xC9;
    }
    GameFingerprint.Match match = index.identify(poked);
    assertEquals("manicminer", match.game().title);
    assertTrue(match.score() > 0.8, "score: " + match.score());
  }

  @Test
  void anUnknownGameIsNotForcedIntoTheCatalogue() {
    assertNull(index.identify(noise(1234, game.length)));
  }

  @Test
  void theIndexSurvivesBeingWrittenAndReadBack(@org.junit.jupiter.api.io.TempDir Path directory) throws IOException {
    Path saved = directory.resolve("catalogue.json");
    index.save(saved);
    GameFingerprint.Index reloaded = new GameFingerprint.Index();
    reloaded.load(saved);
    assertEquals("manicminer", reloaded.identify(game).game().title);
    assertEquals(1.0, reloaded.identify(game).score());
  }

  /**
   * Against a real collection, where the same game turns up as TAP, TZX, Z80 and SNA. There are no
   * labels saying which files are the same game, so what is measured is how many land in the band
   * where the score decides nothing.
   * <p>
   * The band is not empty and cannot be made empty. Read one by one, almost everything in it is a
   * real relative: side 1 against side 2 of the same tape, part 1 against part 2, a tape against a
   * snapshot of the same game - all of which share a loader and some data without sharing an
   * image. Measured over 1091 files, 11% land between 0.15 and 0.45. What this test guards is that
   * the share does not grow; which of those are relatives and which are accidents is a question
   * for eyes, not for an assertion.
   */
  @Test
  @EnabledIfSystemProperty(named = "oozx.games", matches = ".+")
  void onARealCollectionTheUndecidedBandStaysSmall() throws IOException {
    Map<Path, GameFingerprint> collection = new LinkedHashMap<>();
    try (Stream<Path> files = Files.walk(Path.of(System.getProperty("oozx.games")))) {
      for (Path file : files.filter(Files::isRegularFile)
          .filter(file -> file.toString().toLowerCase().matches(".*\\.(tap|tzx|z80|sna|szx)$")).toList()) {
        collection.put(file, GameFingerprint.of(file));
      }
    }
    int undecided = 0;
    for (Path file : collection.keySet()) {
      GameFingerprint.Index others = new GameFingerprint.Index();
      collection.forEach((other, fingerprint) -> {
        if (!other.equals(file)) {
          others.add(GameFingerprint.named(other.toString(), other.getFileName().toString()), fingerprint);
        }
      });
      GameFingerprint.Match match = others.identify(collection.get(file));
      if (match != null && match.score() > 0.15 && match.score() < 0.45) {
        undecided++;
      }
    }
    assertTrue(undecided < collection.size() * 0.15,
        "sin decidir: " + undecided + " de " + collection.size());
  }

  private static byte[] noise(int seed, int length) {
    byte[] bytes = new byte[length];
    new Random(seed).nextBytes(bytes);
    return bytes;
  }

  private static byte[] concat(byte[] first, byte[] second) {
    byte[] both = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, both, first.length, second.length);
    return both;
  }
}

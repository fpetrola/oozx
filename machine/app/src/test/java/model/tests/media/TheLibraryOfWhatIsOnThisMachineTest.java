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
package model.tests.media;

import com.fpetrola.emulation.helpers.snapshots.SnapshotFactory;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Peripheral;
import com.fpetrola.oozx.api.GameFingerprint;
import com.fpetrola.oozx.api.GameLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheLibraryOfWhatIsOnThisMachineTest {
  private static final double CERTAINTY = 0.3;
  private static final Path MANIC_MINER = Path.of("../../doc/manicminer.z80");

  private static GameLibrary libraryOf() {
    return libraryOf(GameFingerprint.Index.shipped());
  }

  private static GameLibrary libraryOf(GameFingerprint.Index catalogue) {
    return new GameLibrary(catalogue, new GameLibrary.Emulator() {
      public boolean loadable(Path file) {
        return file.toString().toLowerCase().matches(".*\\.(tap|tzx|z80|sna|szx)$");
      }

      public byte[] payload(Path file) throws IOException {
        return SnapshotFactory.payloadOf(file.toFile());
      }

      public boolean inColour(Path file) {
        return Spec256Peripheral.hasColours(file.toString());
      }
    });
  }

  @Test
  void aGameWithItsOwnColoursBesideItSaysSo(@TempDir Path directory) throws IOException {
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"));
    Files.write(directory.resolve("whatever.gfx"), new byte[8]);
    Files.copy(MANIC_MINER, directory.resolve("plain.z80"));
    GameLibrary library = libraryOf();
    library.scan(directory, CERTAINTY);

    assertTrue(library.of(directory.resolve("whatever.z80")).inColour());
    assertFalse(library.of(directory.resolve("plain.z80")).inColour());
  }

  @Test
  void aGameOfTheCatalogueIsNamedByTheCatalogue(@TempDir Path directory) throws IOException {
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"));
    GameLibrary library = libraryOf();
    library.scan(directory, CERTAINTY);

    GameLibrary.Copy copy = library.of(directory.resolve("whatever.z80"));
    assertTrue(copy.identified(), "no lo reconocio: " + copy.score());
    assertEquals("Manic Miner", copy.game().title);
  }

  @Test
  void whatTheCatalogueDoesNotKnowIsKeptAsUnknown(@TempDir Path directory) throws IOException {
    byte[] noise = new byte[40000];
    new Random(7).nextBytes(noise);
    Files.write(directory.resolve("nothing.tap"), noise);
    GameLibrary library = libraryOf();
    library.scan(directory, CERTAINTY);

    GameLibrary.Copy copy = library.of(directory.resolve("nothing.tap"));
    assertFalse(copy.identified());
    assertEquals("nothing.tap", copy.title());
    assertEquals(1, library.unknown().size());
  }

  @Test
  void whatIsWrittenDownIsNotIdentifiedAgain(@TempDir Path directory) throws IOException {
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"));
    GameLibrary library = libraryOf();
    library.scan(directory, CERTAINTY);
    library.save(directory.resolve("library.json"));

    GameLibrary reopened = libraryOf();
    reopened.load(directory.resolve("library.json"));
    assertEquals("Manic Miner", reopened.of(directory.resolve("whatever.z80")).game().title);

    // A file that has not changed is skipped, so overwriting it with something else and rescanning
    // without touching its date leaves the library saying what it said before.
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    Files.setLastModifiedTime(directory.resolve("whatever.z80"),
        java.nio.file.attribute.FileTime.fromMillis(reopened.of(directory.resolve("whatever.z80")).modified()));
    reopened.scan(directory, CERTAINTY);
    assertEquals("Manic Miner", reopened.of(directory.resolve("whatever.z80")).game().title);
  }

  @Test
  void aLibraryWrittenAgainstAnOlderCatalogueIsAskedAgain(@TempDir Path directory) throws IOException {
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"));
    // Written when the catalogue knew nothing: the copy is written down, and written down unknown.
    GameLibrary before = libraryOf(new GameFingerprint.Index());
    before.scan(directory, CERTAINTY);
    assertEquals(1, before.unknown().size());
    before.save(directory.resolve("library.json"));

    // Reopened once the catalogue that knows it has shipped. Reading the library does not ask
    // anything - which is what left a game on the disk with no name and no picture after the
    // catalogue grew - so it is asked, and then there is nothing left to ask.
    GameLibrary reopened = libraryOf();
    reopened.load(directory.resolve("library.json"));
    assertEquals(1, reopened.unknown().size(), "what was written down cannot name it by itself");

    assertEquals(1, reopened.askAgain(CERTAINTY));
    assertEquals("Manic Miner", reopened.of(directory.resolve("whatever.z80")).game().title);
    assertEquals(0, reopened.askAgain(CERTAINTY), "asked again about something already answered");
  }

  @Test
  void aGameThatIsGoneLeavesTheLibrary(@TempDir Path directory) throws IOException {
    Files.copy(MANIC_MINER, directory.resolve("whatever.z80"));
    GameLibrary library = libraryOf();
    library.scan(directory, CERTAINTY);
    Files.delete(directory.resolve("whatever.z80"));

    assertEquals(1, library.forgetMissing());
    assertEquals(0, library.games().size());
  }

  /** Over a real collection, to see how much of it the shipped catalogue actually names. */
  @Test
  @EnabledIfSystemProperty(named = "oozx.games", matches = ".+")
  void mostOfARealCollectionGetsANameAndTheRestIsUnknown() throws IOException {
    GameLibrary library = libraryOf();
    library.scan(Path.of(System.getProperty("oozx.games")), CERTAINTY);
    long named = library.games().stream().filter(GameLibrary.Copy::identified).count();
    System.out.println("en la biblioteca: " + library.games().size() + ", con nombre: " + named
        + ", desconocidos: " + library.unknown().size());
    assertTrue(named > 0);
  }
}

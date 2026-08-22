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

import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.rzx.RzxArchive;
import com.fpetrola.oozx.rzx.RzxRecording;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalogue is data in the repository, so what is worth testing is not that Jackson works
 * but that the join still holds: that a Spectrum Computing id coming out of a game search finds
 * the recordings of that game, and that what comes back is playable.
 */
public class RzxArchiveTest {

  private static final int JET_SET_WILLY_128K = 2594;
  private static final int JABATO = 6493;

  @Test
  public void findsTheRecordingsOfAGameByTheIdTheSearchAlreadyHas() {
    RzxArchive archive = new RzxArchive();

    List<RzxRecording> jsw = archive.recordingsFor(JET_SET_WILLY_128K);
    assertEquals(1, jsw.size(), "expected one recording of Jet Set Willy 128K");

    RzxRecording recording = jsw.get(0);
    assertEquals("Jet Set Willy 128K", recording.title());
    assertEquals("Daniel Gromann", recording.submitter());
    assertTrue(recording.isPlayable());
    assertEquals("https://www.rzxarchive.co.uk/j/jetsetwilly128k.rzx", recording.download().url());
    assertFalse(recording.download().isZipped());

    // Zipped recordings are the other shape a download comes in; the caller has to unzip.
    RzxRecording jabato = archive.recordingsFor(JABATO).get(0);
    assertTrue(jabato.download().isZipped(), "Jabato is distributed as a zip");
  }

  @Test
  public void aGameWithNoRecordingAnswersEmptyRatherThanFailing() {
    RzxArchive archive = new RzxArchive();
    assertTrue(archive.recordingsFor(-1).isEmpty());
    assertFalse(archive.hasRecordings(-1));
  }

  /**
   * Guards the catalogue itself, not the reader. If a regenerated file lost the join key or the
   * download links, everything above would still pass on the few ids it names.
   */
  @Test
  public void theCatalogueIsWholeEnoughToBeWorthJoiningAgainst() {
    Map<Integer, List<RzxRecording>> byGame = new RzxArchive().byGame();

    int recordings = byGame.values().stream().mapToInt(List::size).sum();
    long playable = byGame.values().stream().flatMap(List::stream)
        .filter(RzxRecording::isPlayable).count();

    assertEquals(4280, recordings, "recordings carrying a Spectrum Computing id");
    assertEquals(4256, byGame.size(), "distinct games");
    assertTrue(playable > 4000, "most recordings should be downloadable, got " + playable);

    for (List<RzxRecording> ofOneGame : byGame.values()) {
      for (RzxRecording recording : ofOneGame) {
        assertNotNull(recording.title(), "a recording with no title");
        assertTrue(recording.isPlayable() || recording.distributionDenied(),
            recording.title() + " has nothing to download and is not marked denied");
      }
    }
  }
}

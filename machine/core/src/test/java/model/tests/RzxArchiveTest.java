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

package model.tests;

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

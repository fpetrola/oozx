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

import com.fpetrola.oozx.speccy.peripherals.t.DownloadAndUnzip;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which of an entry's several downloads gets picked.
 * <p>
 * A game in the archive usually has more than one file and they are not interchangeable: a
 * snapshot loads at once, a tape has to be played in, and some of them the archive is not allowed
 * to hand over at all. Picking wrongly looks from the outside like a download that refuses.
 */
class DownloadChoiceTest {

  private static final Function<String, String> WHOLE_URL = url -> url;

  @Test
  void a_tape_only_entry_is_still_something_to_load() {
    // The browser used to keep its own shorter list of formats than the scorer, so an entry
    // offered only as a TAP was dropped before anything ranked it - and dropped in silence.
    assertTrue(DownloadAndUnzip.loadable("https://zxinfo.dk/media/pub/g/Game.tap.zip"),
        "a tape is loadable and was being discarded");
    assertTrue(DownloadAndUnzip.loadable("https://zxinfo.dk/media/pub/g/Game.szx"));
    assertTrue(DownloadAndUnzip.loadable("https://zxinfo.dk/media/pub/g/Game.csw"));
  }

  @Test
  void a_disk_image_is_not_something_this_emulator_opens() {
    // Which is the honest answer for the first thing a search for "dizz" finds: its only file is
    // a TR-DOS disk, and there is no loader for one.
    assertFalse(DownloadAndUnzip.loadable(
        "https://zxinfo.dk/media/pub/sinclair/trdos/games/d/DizzyElusive.trd.zip"));
  }

  @Test
  void what_the_archive_may_not_hand_over_comes_last() {
    // ZXDB files it cannot distribute under /denied/. A tape that will actually come down beats
    // a perfect tape that will answer with a refusal.
    String denied = "https://zxinfo.dk/media/denied/entries/0011243/Game.tzx.zip";
    String plain = "https://zxinfo.dk/media/pub/sinclair/games/g/Game.tap.zip";
    assertEquals(plain, DownloadAndUnzip.preferred(List.of(denied, plain), WHOLE_URL),
        "chose the one the archive will not give out");
    // With nothing else on offer it is still the answer: better a refusal that explains itself
    // than pretending the entry has nothing.
    assertEquals(denied, DownloadAndUnzip.preferred(List.of(denied), WHOLE_URL));
  }

  @Test
  void a_snapshot_beats_a_tape_and_48k_beats_128k() {
    String tape = "https://zxinfo.dk/media/pub/g/Game.tzx.zip";
    String snapshot = "https://zxinfo.dk/media/pub/g/Game.z80.zip";
    assertEquals(snapshot, DownloadAndUnzip.preferred(List.of(tape, snapshot), WHOLE_URL));

    String for128 = "https://zxinfo.dk/media/pub/g/Game128.z80.zip";
    String for48 = "https://zxinfo.dk/media/pub/g/Game48.z80.zip";
    assertEquals(for48, DownloadAndUnzip.preferred(List.of(for128, for48), WHOLE_URL),
        "the emulator boots a 48K machine");
  }

  @Test
  void what_the_archive_withholds_is_known_before_asking_for_it() {
    // So the filter for "things I can load" can leave it out, and a click on it can say why
    // without spending a download to find out.
    assertFalse(DownloadAndUnzip.available(
        "https://zxinfo.dk/media/denied/entries/0011243/DizzyCollection.tzx.zip"));
    assertTrue(DownloadAndUnzip.available(
        "https://zxinfo.dk/media/pub/sinclair/games/d/DizzyCollection.tzx.zip"));
    assertFalse(DownloadAndUnzip.available(null), "nothing at all is not available either");
  }

  @Test
  void the_list_offered_is_in_the_order_it_would_have_chosen() {
    // The submenu that lets somebody take the 128K release instead of the 48K one is built from
    // this, so the top of that list has to be what would have been picked for them. Two rules
    // that disagree would put the default in the middle of the menu.
    String tape = "https://zxinfo.dk/media/pub/g/Game.tzx.zip";
    String snapshot = "https://zxinfo.dk/media/pub/g/Game.z80.zip";
    String for128 = "https://zxinfo.dk/media/pub/g/Game128.tzx.zip";
    java.util.List<String> ordered =
        DownloadAndUnzip.byPreference(List.of(for128, tape, snapshot), WHOLE_URL);
    assertEquals(snapshot, ordered.get(0), "the list does not start with what it would choose");
    assertEquals(DownloadAndUnzip.preferred(ordered, WHOLE_URL), ordered.get(0),
        "the order and the choice disagree");
    assertEquals(3, ordered.size(), "the list lost one on the way");
  }

  @Test
  void nothing_loadable_answers_nothing() {
    assertNull(DownloadAndUnzip.preferred(
        List.of("https://zxinfo.dk/media/pub/g/Game.trd.zip",
            "https://zxinfo.dk/media/pub/g/Cover.jpg"), WHOLE_URL),
        "answered with something it cannot open");
  }
}

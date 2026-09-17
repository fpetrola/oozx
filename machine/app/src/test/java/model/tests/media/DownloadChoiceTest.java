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

import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;
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
  void the_plain_tosec_dump_beats_the_ones_somebody_got_at() {
    // TOSEC names one release many times over: hacked, cracked, translated, re-dumped. Any of
    // them loads, but the fingerprint catalogue is built from whichever is chosen, so a game
    // recorded from [h Byte Rus] is recorded as something nobody else has.
    String plain = "https://archive.org/download/set/Games.zip/Games/1942/1942%20(1986)(Elite%20Systems).tap";
    String hacked = "https://archive.org/download/set/Games.zip/Games/1942/1942%20(1986)(Elite%20Systems)%5Bh%20Byte%20Rus%5D.tap";
    assertEquals(plain, DownloadAndUnzip.preferred(List.of(hacked, plain), WHOLE_URL),
        "took the copy somebody else had already changed");
    // And it outweighs the format: a snapshot loads without a tape's timing, but Commando as
    // somebody else re-dumped it is not what a search for Commando is meant to find.
    String alternate = "https://archive.org/download/set/Games.zip/Games/Commando/Commando%20(1985)(Elite%20Systems)%5Ba2%5D.z80";
    String tape = "https://archive.org/download/set/Games.zip/Games/Commando/Commando%20(1985)(Elite%20Systems).tzx";
    assertEquals(tape, DownloadAndUnzip.preferred(List.of(alternate, tape), WHOLE_URL),
        "a marked snapshot beat the plain tape");
  }

  @Test
  void a_file_is_named_the_way_it_reads_and_not_the_way_a_url_spells_it() {
    // The menu that offers the versions of a game is built from these, and a TOSEC name arrives
    // with every space and bracket escaped: "Knight%20Lore%20(1984)(Ricochet)%5Bre-release%5D.tzx".
    assertEquals("Knight Lore (1984)(Ricochet)[re-release].tzx", DownloadAndUnzip.nameOf(
        "https://archive.org/download/set/Games.zip/Games/Knight%20Lore/Knight%20Lore%20(1984)(Ricochet)%5Bre-release%5D.tzx"));
    // A plus is a plus in a path, not a space: Pac-Man Emulator is published as _+2A_+3.tap.
    assertEquals("Pac-ManEmulator_v1.6_+2A_+3.tap",
        DownloadAndUnzip.nameOf("https://zxinfo.dk/media/pub/p/Pac-ManEmulator_v1.6_+2A_+3.tap"));
    // And a name that never went through a URL comes back untouched, escaping or not.
    assertEquals("Head Over Heels .tap", DownloadAndUnzip.nameOf("/tmp/games/Head Over Heels .tap"));
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
    assertNull(DownloadAndUnzip.chooseLoadable(List.of()),
        "a folder with nothing in it was answered with its first file");
  }
}

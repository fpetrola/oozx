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

import com.fpetrola.oozx.api.GameEntry;
import com.fpetrola.oozx.api.GameFile;
import com.fpetrola.oozx.api.Release;
import com.fpetrola.oozx.api.ZxInfoApiHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some of the entries anybody wants - Knight Lore, Jetpac, every Dizzy - are ones ZXDB holds but may
 * not hand out, and it lists those under /denied/, where nothing answers. The same entry also says
 * where TOSEC keeps the file, so that is what is offered once nothing else is left.
 */
class WhatAnEntryOffersToDownloadTest {
  private static final String ENDURO = "/Games/Enduro Racer/Enduro Racer (1987)(Activision)(48K-128K).tzx";
  private static final String KNIGHT_LORE = "/Games/Knight Lore/Knight Lore (1984)(Ricochet)[re-release].tzx";

  /** Standing in for the emulator: the formats it opens, which is not the database's business. */
  private static final java.util.function.Predicate<String> LOADABLE =
      url -> url.replace(".zip", "").matches("(?i).*\\.(tzx|tap|z80|sna|szx|csw)$");

  private static GameEntry entry(List<String> tosec, String... paths) {
    GameEntry entry = new GameEntry();
    Release release = new Release();
    release.files = new ArrayList<>();
    for (String path : paths) {
      GameFile file = new GameFile();
      file.path = path;
      file.format = "Perfect tape (TZX)";
      release.files.add(file);
    }
    entry.releases = List.of(release);
    entry.tosec = tosec.stream().map(path -> {
      GameEntry.TosecEntry known = new GameEntry.TosecEntry();
      known.path = path;
      return known;
    }).toList();
    return entry;
  }

  @Test
  void whatTheArchiveMayNotHandOverIsOfferedFromTosecInstead() {
    // Enduro Racer's tape is withheld, but the same entry lists two magazine scans ZXDB does serve.
    // Asking whether the whole list was withheld answered no, and left the game with no tape.
    Map<String, String> offers = ZxInfoApiHandler.filesOf(
        entry(List.of(ENDURO), "/denied/entries/0001628/EnduroRacer.tzx.zip",
            "/pub/sinclair/magazines/snippets/ElTebeoInformatico/EnduroRacer-1.jpg"), LOADABLE);

    assertEquals(3, offers.size(), "the denied file is kept, so a refusal can still explain itself");
    assertTrue(offers.containsKey(ZxInfoApiHandler.tosecUrl(ENDURO)),
        "an entry ZXDB may not serve was left with nowhere to download it from");
  }

  @Test
  void anEntryWithNothingThisMachineCouldUseIsAlsoOfferedTosec() {
    // Karlos is not withheld at all: ZXDB serves it, only as a TR-DOS disk this emulator cannot
    // open. Soft Aid is withheld and does not even list a file to be withheld. Asking whether
    // anything had been withheld said no to both, and that was twelve games of the 5000 most voted.
    Map<String, String> onlyADisk = ZxInfoApiHandler.filesOf(
        entry(List.of(ENDURO), "/pub/sinclair/trdos/games/k/Karlos.trd.zip"), LOADABLE);
    assertTrue(onlyADisk.containsKey(ZxInfoApiHandler.tosecUrl(ENDURO)),
        "a disk nothing here can read counts as having nothing");

    Map<String, String> notEvenAFile = ZxInfoApiHandler.filesOf(entry(List.of(ENDURO)), LOADABLE);
    assertEquals(1, notEvenAFile.size(), "an entry listing no file at all was left with none");
  }

  @Test
  void anEntryTheArchiveDoesServeIsNotFilledWithTosecVariants() {
    // TOSEC lists sixteen dumps of Knight Lore alone. They are a way out of a refusal, not a
    // longer menu for a game that already comes down.
    Map<String, String> offers = ZxInfoApiHandler.filesOf(
        entry(List.of(ENDURO), "/zxdb/sinclair/entries/0002259/HeadOverHeels.tzx.zip"), LOADABLE);

    assertEquals(1, offers.size(), "the browser's list of files grew for no reason");
  }

  @Test
  void onlyAnEntryWithSomethingWithheldCostsASecondCall() {
    // The whole entry is a call to somebody else's server, and a search is 150 hits: asking for
    // every one of them would trade a search for two and a half minutes of them. Both of these
    // answer from what the search already returned, without reaching for the network.
    ZxInfoApiHandler api = new ZxInfoApiHandler();
    GameEntry served = entry(List.of(), "/zxdb/sinclair/entries/0002259/HeadOverHeels.tzx.zip");
    assertSame(served, api.withTosecFiles("0002259", served), "asked again for an entry it can serve");

    GameEntry complete = entry(List.of(ENDURO), "/denied/entries/0001628/EnduroRacer.tzx.zip");
    assertSame(complete, api.withTosecFiles("0001628", complete), "asked again for what it already had");
  }

  @Test
  void whatWasFoundElsewhereSaysSo() {
    // The versions menu lists both kinds together, and one that exists only because it was found
    // in a TOSEC set is a different thing to pick than one the archive itself hands out.
    assertTrue(ZxInfoApiHandler.fromTosec(ZxInfoApiHandler.tosecUrl(ENDURO)));
    assertFalse(ZxInfoApiHandler.fromTosec("https://worldofspectrum.net/pub/sinclair/games/h/HeadOverHeels.tap.zip"));
    assertFalse(ZxInfoApiHandler.fromTosec(null));
  }

  @Test
  void aTosecPathIsServedFromTheZipItsFirstFolderNames() {
    // Spaces, commas and the brackets TOSEC marks its variants with all have to survive the trip.
    assertEquals("https://archive.org/download/zx_spectrum_tosec_set_september_2023/Games.zip"
            + "/Games/Knight%20Lore/Knight%20Lore%20(1984)(Ricochet)%5Bre-release%5D.tzx",
        ZxInfoApiHandler.tosecUrl(KNIGHT_LORE));
    assertTrue(ZxInfoApiHandler.tosecUrl("/Compilations/Games/Dizzy Collection, The/x.tzx")
        .contains("/Compilations.zip/Compilations/Games/Dizzy%20Collection,%20The/"));
    // A plus and an ampersand are legal in a path and are not left in one: what serves a file out
    // of the zip reads them as a query would, and answered 503 for both until they were escaped.
    assertTrue(ZxInfoApiHandler.tosecUrl("/Games/Olli & Lissa 3/Olli & Lissa 3 (1989).tap")
        .endsWith("/Games/Olli%20%26%20Lissa%203/Olli%20%26%20Lissa%203%20(1989).tap"));
    assertTrue(ZxInfoApiHandler.tosecUrl("/Compilations/Games/1942 + Batty/1942 + Batty (1989).tzx")
        .endsWith("/1942%20%2B%20Batty/1942%20%2B%20Batty%20(1989).tzx"));
  }
}

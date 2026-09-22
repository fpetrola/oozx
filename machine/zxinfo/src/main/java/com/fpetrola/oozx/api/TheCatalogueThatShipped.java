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

package com.fpetrola.oozx.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the emulator knows about games without asking anybody: the catalogue of five thousand
 * that ships inside it, which was already there to put a name on a file and is read here to
 * answer the same questions the web service does.
 * <p>
 * It knows a title, a year, a publisher and a screenshot, and nothing else - the paths the files
 * are served from are not in it, so an entry this answers with offers nothing to download. That
 * is the whole difference, and it is why this is asked second.
 */
public class TheCatalogueThatShipped implements KnowsTheGames {

  /** Read once and kept: four thousand nine hundred entries out of a file in the jar. */
  private static GameFingerprint.Index catalogue;

  private static synchronized GameFingerprint.Index catalogue() {
    if (catalogue == null) {
      catalogue = GameFingerprint.Index.shipped();
    }
    return catalogue;
  }

  @Override
  public String where() {
    return "the catalogue in this build";
  }

  /**
   * Every title that has all the words of the query in it. Not the web service's scoring: that
   * ranks over everything ever published and this has a title and nothing else to rank with.
   * <p>
   * All the words rather than the whole phrase, because what somebody types is the name as they
   * remember it: "everyone wally" is how one asks for "Everyone's a Wally", and a search for the
   * phrase finds nothing. A title that begins with what was asked comes first all the same.
   */
  @Override
  public List<Hit> search(String query, String machineType, String genreType) {
    String asked = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String[] words = asked.isEmpty() ? new String[0] : asked.split("\\s+");
    List<Hit> starting = new ArrayList<>();
    List<Hit> rest = new ArrayList<>();
    for (GameFingerprint.Known known : catalogue().all()) {
      String title = known.game().title == null ? "" : known.game().title.toLowerCase(Locale.ROOT);
      if (!hasEveryWord(title, words)) {
        continue;
      }
      (title.startsWith(asked) ? starting : rest).add(hitOf(known));
    }
    starting.addAll(rest);
    return starting;
  }

  private static boolean hasEveryWord(String title, String[] words) {
    for (String word : words) {
      if (!title.contains(word)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public GameEntry game(String id) {
    GameFingerprint.Known known = catalogue().known(id);
    return known == null ? null : entryOf(known);
  }

  private static Hit hitOf(GameFingerprint.Known known) {
    Hit hit = new Hit();
    hit._id = known.game().id;
    hit._source = entryOf(known);
    return hit;
  }

  /**
   * The summary said in the terms an entry is said in, so that whoever reads the answer cannot
   * tell where it came from. SOFTWARE because that is what a catalogue of games holds, and the
   * browser drops anything else.
   */
  private static GameEntry entryOf(GameFingerprint.Known known) {
    GameSummary game = known.game();
    GameEntry entry = new GameEntry();
    entry._id = game.id;
    entry.id = game.id;
    entry.contentType = "SOFTWARE";
    entry.title = game.title;
    entry.originalYearOfRelease = yearOf(game.yearOfRelease);
    entry.publishers = game.publisher == null ? List.of() : List.of(publisher(game.publisher));
    entry.screens = new ArrayList<>();
    if (known.screenshot() != null) {
      // The shape a screen comes back in, so that whoever reads one does not have to know that
      // this answer was not the web service's.
      entry.screens.add(java.util.Map.of("url", known.screenshot(), "type", "Loading screen"));
    }
    return entry;
  }

  private static Integer yearOf(String said) {
    try {
      return said == null || said.isBlank() ? null : Integer.valueOf(said.trim());
    } catch (NumberFormatException notAYear) {
      return null;
    }
  }

  private static Publisher publisher(String name) {
    Publisher publisher = new Publisher();
    publisher.name = name;
    return publisher;
  }
}

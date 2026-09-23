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

import com.fpetrola.oozx.TellsThePerson;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Asks whoever can answer about a game, in the order that gives the best answer first.
 * <p>
 * The web service knows everything and is asked first. When it cannot be reached ZXDB answers
 * from this machine instead, in the same shape and with the same files to download: fewer games
 * until the whole of it has been brought down, but nobody reading the answer has to know which
 * one gave it.
 * <p>
 * One that failed is not asked again for a while. A search that waits twenty seconds for a
 * connection nobody is going to accept, and does it on every keystroke, is a window that hangs.
 */
public final class Catalogues {

  /** Long enough that a run of searches does not each wait for the same refusal. */
  private static final long REST_AFTER_FAILING = java.time.Duration.ofMinutes(5).toMillis();

  private static final java.util.Map<String, Long> refused = new java.util.concurrent.ConcurrentHashMap<>();

  /** Who is asked, in order. Found rather than named, so a third can arrive in a jar. */
  public static List<KnowsTheGames> all() {
    List<KnowsTheGames> asking = new ArrayList<>();
    asking.add(new ZxInfoApiHandler());
    asking.add(new Zxdb());
    com.fpetrola.oozx.plugins.Plugins.found(KnowsTheGames.class).stream()
        .filter(one -> asking.stream().noneMatch(already -> already.getClass() == one.getClass()))
        .forEach(asking::add);
    return asking;
  }

  public static List<Hit> search(String query, String machineType, String genreType) {
    List<Hit> found = ask(who -> who.search(query, machineType, genreType));
    return found == null ? List.of() : found;
  }

  public static GameEntry game(String id) {
    return ask(who -> who.game(id));
  }

  public static GameDetail details(String id) {
    return ask(who -> who.details(id));
  }

  public static Metadata metadata() {
    return ask(KnowsTheGames::metadata);
  }

  /**
   * The entry with what a compact search leaves out. A search answers without the TOSEC paths, so
   * an entry ZXDB withholds arrives looking like one with nothing to download at all, and stays
   * that way until the whole entry is asked for.
   * <p>
   * This asks a cheaper question than {@link ZxInfoApiHandler#filesOf} does, and on purpose. What
   * deserves TOSEC is every entry with nothing usable here; what deserves a second call over the
   * network is the narrower "something was withheld", because on a search for "r-type" the first
   * is 42 of the 138 hits and the second is one of them. The catalogue builder pays neither: it
   * holds whole entries.
   */
  public static GameEntry withTosecFiles(String id, GameEntry game) {
    if (game.tosec != null || !anythingWithheld(game)) {
      return game;
    }
    GameEntry whole = game(id);
    return whole == null ? game : whole;
  }

  /** Whether the entry names a file ZXDB may not hand out, read straight off the paths it lists. */
  private static boolean anythingWithheld(GameEntry game) {
    return game.releases != null && game.releases.stream().anyMatch(release -> release.files != null
        && release.files.stream().anyMatch(file -> ZxInfoApiHandler.denied(file.path)));
  }

  /**
   * The first answer there is. A refusal moves on to the next and is said once, since what the
   * person sees is a shorter list and nothing about it explains why.
   */
  private static <T> T ask(Function<KnowsTheGames, T> question) {
    RuntimeException firstRefusal = null;
    for (KnowsTheGames who : all()) {
      if (!who.canAnswer() || restingAfterFailing(who)) {
        continue;
      }
      try {
        T answer = question.apply(who);
        if (answer != null) {
          refused.remove(who.where());
          return answer;
        }
      } catch (RuntimeException wouldNotAnswer) {
        refused.put(who.where(), System.currentTimeMillis());
        TellsThePerson.that(who.where() + " could not be asked, so what is known here answers instead: "
            + rootCauseOf(wouldNotAnswer));
        if (firstRefusal == null) {
          firstRefusal = wouldNotAnswer;
        }
      }
    }
    return null;
  }

  private static boolean restingAfterFailing(KnowsTheGames who) {
    Long failedAt = refused.get(who.where());
    return failedAt != null && System.currentTimeMillis() - failedAt < REST_AFTER_FAILING;
  }

  private static String rootCauseOf(Throwable wrong) {
    while (wrong.getCause() != null) {
      wrong = wrong.getCause();
    }
    return wrong.getMessage() == null ? wrong.toString() : wrong.getMessage();
  }

  private Catalogues() {
  }
}

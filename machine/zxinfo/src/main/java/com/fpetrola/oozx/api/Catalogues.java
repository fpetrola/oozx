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
 * The web service knows everything and is asked first, because it is the only one that says
 * where the files of an entry are. When it cannot be reached the catalogue in this build
 * answers instead: less, but a name and a year and five thousand games beat a window that says
 * nothing at all because a host was down.
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
    asking.add(new TheCatalogueThatShipped());
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

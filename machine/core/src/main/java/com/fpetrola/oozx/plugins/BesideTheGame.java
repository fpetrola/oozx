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

package com.fpetrola.oozx.plugins;

/**
 * Something a game carries beside its own file, recognised by whoever can read it.
 * <p>
 * The library and the browser say that a game has it without knowing what it is: the only one who
 * knows that a {@code .gfx} beside a snapshot is a set of colours is the board that paints them,
 * and that board is a jar that may not even be here.
 */
@Plugin("extra")
public interface BesideTheGame {

  /** What it is, in the words the person reading it will see: "256 colors". */
  String what();

  /** Whether this game has it beside it. */
  boolean isBeside(String game);

  /** What this game brings, if anything here can recognise it. */
  static String whatIsBeside(String game) {
    for (BesideTheGame kind : Plugins.found(BesideTheGame.class)) {
      if (kind.isBeside(game)) return kind.what();
    }
    return null;
  }

  static boolean anythingBeside(String game) {
    return whatIsBeside(game) != null;
  }
}

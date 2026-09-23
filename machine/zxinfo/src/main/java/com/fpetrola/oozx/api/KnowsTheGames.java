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

import dev.crystal.plugins.api.RoleInterface;


import java.util.List;

/**
 * Somebody who knows what games there are: what is called what, when, by whom, and where the
 * files of it are.
 * <p>
 * There used to be one, the ZXInfo API, named by whoever wanted something from it. The day it
 * answered 502 the game browser had nothing to say and no other place to ask, although the
 * catalogue of five thousand games ships inside the emulator and the files of them were still
 * being served. This is the seam that lets the second one answer when the first cannot.
 */
@RoleInterface
public interface KnowsTheGames {

  /** What to call this where somebody is told which one answered. */
  String where();

  /** Whether it is worth asking at all: a file that is not there, a host that just refused. */
  default boolean canAnswer() {
    return true;
  }

  /** What is known about the games whose title says this. A filter left null is not applied. */
  List<Hit> search(String query, String machineType, String genreType);

  /** Everything about one entry, which is where the files it can be downloaded from are listed. */
  GameEntry game(String id);

  /** The same entry as the windows show it. */
  default GameDetail details(String id) {
    GameEntry entry = game(id);
    return entry == null ? null : GameDetail.of(entry, id);
  }
}

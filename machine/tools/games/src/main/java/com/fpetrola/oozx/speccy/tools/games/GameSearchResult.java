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

package com.fpetrola.oozx.speccy.tools.games;

import com.fpetrola.oozx.rzx.RzxOption;

/** A row of the list: what the catalogue or the disk says about one game. */
public class GameSearchResult {
  public String id;
  String title;
  String url;
  String screenshot1;
  String screenshot2;
  String filename;
  /** What the entry offers that cannot be loaded, for saying so instead of "no tape". */
  String offers;
  /** Whether the chosen file can be expected to come down; false for what the archive withholds. */
  boolean available;
  /** Every file the entry offers that could be loaded, best first, for choosing among them. */
  java.util.List<String> files = java.util.List.of();
  /** Extras the entry carries, for filters the server cannot apply itself. */
  boolean hasRzx;
  boolean hasMap;
  /** The machine somebody picked for it, or null to let the file and its name decide. */
  String machine;
  /** Whether this is a file already on the disk rather than an entry to fetch. */
  boolean onThisMachine;
  /** How many copies of this game were found, which for one on the net is the entry itself. */
  int copies = 1;
  /** Whether the copy on this machine has Spec256's own colours beside it. */
  boolean inColour;
  /** The year and publisher, or what the file is called when the catalogue did not know it. */
  String subtitle;
  /** Recordings of this game offered for playing, from both catalogues. */
  java.util.List<RzxOption> recordings = java.util.List.of();

  public GameSearchResult(String _id, String title, String url, String screenshot1, String screenshot2, String filename) {
    id = _id;
    this.title = title;
    this.url = url;
    this.screenshot1 = screenshot1;
    this.screenshot2 = screenshot2;
    this.filename = filename;
  }
}

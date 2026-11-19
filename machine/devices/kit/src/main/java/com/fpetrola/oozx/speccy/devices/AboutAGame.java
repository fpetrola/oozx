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

package com.fpetrola.oozx.speccy.devices;

import dev.crystal.plugins.api.RoleInterface;

import javax.swing.JComponent;

/**
 * Something a plugin has of one game, for whoever shows what is known of it: what it calls them, how many there
 * are, and the page where they are seen and used. Whoever shows the game does not know what a poke or a map is;
 * each plugin brings its own, and taking the plugin out takes its part with it.
 */
@RoleInterface
public interface AboutAGame {
  String what();

  /** How many of them that game has; with none, nothing is shown of this. */
  int howMany(Desk.Game game);

  JComponent page(Desk.Game game);
}

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
package com.fpetrola.oozx.rzx;

/**
 * A recording of a game, offered for playing, and where it was found.
 * <p>
 * Two catalogues list them and neither contains the other: over 329 games from three searches,
 * 26 were in both, 15 only in what ZXDB hosts and 108 only in the RZX Archive. So both are asked
 * and the answers put together, with each one saying where it came from - the Archive knows who
 * recorded it, which ZXDB does not.
 *
 * @param label what to show in the menu
 * @param url   where to fetch it, a .rzx or a .zip holding one
 */
public record RzxOption(String label, String url) {
}

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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;

/**
 * A window that comes back where it was left: it says what to write down and reads it again.
 * <p>
 * The desk saves this for whatever window offers it, so a window that was found rather than
 * named still reopens where and how it was.
 */
public interface KeepsItsPlace {

  OOZxConfiguration.WindowState saveWindowState();

  void restoreWindowState(OOZxConfiguration.WindowState state);
}

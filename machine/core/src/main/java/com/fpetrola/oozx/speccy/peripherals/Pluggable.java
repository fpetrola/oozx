/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
package com.fpetrola.oozx.speccy.peripherals;

/**
 * A peripheral somebody plugs in from outside, by clipping its window onto the machine - as
 * against one a machine comes with, or one a setting asks for.
 */
public interface Pluggable {
  /** The cable. Takes effect at the next update, which is the emulator's own thread's business. */
  void plugIn(boolean connected);

  boolean isPluggedIn();
}

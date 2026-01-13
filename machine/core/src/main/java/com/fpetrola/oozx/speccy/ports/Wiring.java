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


package com.fpetrola.oozx.speccy.ports;

/**
 * Which of the sixteen address lines a device is wired to watch, and what it needs to see on them.
 * It is not the device's: the same chip is soldered differently in different machines, and a
 * Kempston is only on 0x1f because of how it was put in.
 */
@FunctionalInterface
public interface Wiring {
  static Wiring lines(int mask, int value) {
    return port -> (port & mask) == value;
  }

  boolean answers(int port);
}

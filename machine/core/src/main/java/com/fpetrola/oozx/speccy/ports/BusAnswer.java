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

package com.fpetrola.oozx.speccy.ports;

/**
 * What a device answers when a port is read: the byte it puts on the bus, and whether it drove the
 * bus at all. A port nothing answers is left floating, and the machine says what a floating bus
 * reads as.
 * <p>
 * This used to be a byte returned and a one-element array the caller passed in for the second
 * half. There are only 256 bytes a device can drive, so the
 * answers are made once: what a read costs then does not depend on whether the JIT saw through
 * the call that returned it, which at a port read eight times a frame it was measured not to.
 */
public record BusAnswer(int value, boolean driven) {

  /** Nobody answered: all ones, and nothing on the bus. */
  public static final BusAnswer NONE = new BusAnswer(0xff, false);

  private static final BusAnswer[] DRIVEN = new BusAnswer[0x100];

  static {
    for (int value = 0; value < DRIVEN.length; value++) {
      DRIVEN[value] = new BusAnswer(value, true);
    }
  }

  /** That byte, driven onto the bus. */
  public static BusAnswer of(int value) {
    return DRIVEN[value & 0xff];
  }
}

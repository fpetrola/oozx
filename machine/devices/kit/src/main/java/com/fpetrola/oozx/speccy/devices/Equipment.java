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


/**
 * A piece of equipment this build offers, which is what the desk lists in its menu.
 * <p>
 * Found through META-INF/services, so the desk never names one: a device is a jar, and the jar
 * says what it is called and how to open it.
 */
@RoleInterface
public interface Equipment {
  String name();

  /** A window clipped to a machine; a device's window is one of these that has a device too. */
  MachineFrame open();

  /**
   * Whether a window of this kind can be handed that file, which is how the desk finds who opens
   * a cassette or a recording. Nothing can, unless it says so and its window is an {@link Opens}.
   */
  default boolean opens(java.io.File file) {
    return false;
  }
}

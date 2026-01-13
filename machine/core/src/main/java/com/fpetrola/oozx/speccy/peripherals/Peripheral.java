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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.ports.Wired;

public interface Peripheral {
  /** Switched on for the machine that is running, which is the only one it will answer for. */
  void activate(SpectrumMachine machine);

  /** Switched off: put back whatever activating took. */
  void deactivate();

  Wired[] getPorts();

  /**
   * Whether this device belongs on that machine. Asked in what a machine can do, never in which
   * machine it is; a device that names no machine fits none, which is how one arrives switched off
   * until it says where it goes.
   */
  boolean fitsOn(SpectrumMachine machine);

  /**
   * Whether whoever is using this emulator has asked for this one.
   * <p>
   * Only tells apart the devices that are plugged in by choice: one a machine comes with is always
   * wanted. It answers from the settings flag itself rather than from a copy somebody has to
   * remember to keep up to date.
   */
  boolean isWanted();

  boolean hasHardReset();

  /**
   * The machine was reset; put this device back to where it starts.
   * <p>
   * A notification and not an order - it is said to every device that is switched on, after the
   * machine has reset itself, and some have nothing to do about it, which is why there is a
   * default.
   */
  default void machineWasReset(boolean hard) {
  }
}

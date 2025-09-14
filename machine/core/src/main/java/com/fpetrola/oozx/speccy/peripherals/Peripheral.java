/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
   * wanted. In C this is a pointer to the settings flag; this answers from it, which is the same
   * thing said in a language that has no pointers.
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

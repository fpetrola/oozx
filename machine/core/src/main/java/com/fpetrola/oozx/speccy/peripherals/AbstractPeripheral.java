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

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.ports.Wired;

import java.util.List;

public class AbstractPeripheral implements Peripheral {
  private Wired[] portHandlers;

  public AbstractPeripheral(List<Wired> portHandlers) {
    this.portHandlers = portHandlers.toArray(new Wired[0]);
  }

  public void activate(SpectrumMachine machine) {
  }

  public void deactivate() {
  }

  /**
   * For a peripheral whose ports talk back to it: they cannot be handed to super, because
   * there is no this to give them yet.
   */
  protected void ports(Wired... handlers) {
    this.portHandlers = handlers;
  }

  public Wired[] getPorts() {
    return portHandlers;
  }

  /** Built in: on the board of a machine that names it. One that is plugged in says where it fits. */
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.onBoard().contains(getClass());
  }

  /** Built in: nobody is asked. One that is plugged in by choice answers from what was chosen. */
  public boolean isWanted() {
    return true;
  }

  public boolean hasHardReset() {
    return false;
  }
}

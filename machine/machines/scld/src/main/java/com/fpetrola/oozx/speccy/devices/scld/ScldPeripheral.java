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

package com.fpetrola.oozx.speccy.devices.scld;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/** The display register of a Timex machine, wired to port 0xff and to nothing else. */
@Singleton
public class ScldPeripheral extends AbstractPeripheral {
  private final ScldPortHandler port;

  @Inject
  public ScldPeripheral(ScldPortHandler port) {
    super(List.of(Wired.at(0x00ff, 0x00ff, port)));
    this.port = port;
  }

  /** The pair its register names, which is what a wide picture is drawn in. */
  public byte pairOfColours() {
    return port.pairOfColours();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    port.reset();
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.onBoard().contains(ScldPeripheral.class);
  }
}

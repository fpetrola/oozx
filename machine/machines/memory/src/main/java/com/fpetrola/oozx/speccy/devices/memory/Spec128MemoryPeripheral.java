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

package com.fpetrola.oozx.speccy.devices.memory;


import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;


import java.util.List;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.machine.Paging128;
import com.google.inject.Inject;

@com.google.inject.Singleton
public class Spec128MemoryPeripheral extends AbstractPeripheral {
  private Paging128 machine;

  @Inject
  public Spec128MemoryPeripheral() {
    super(List.of());
    ports(Wired.at(0x8002, 0x0000, new Spec128PortHandler(() -> machine)));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    this.machine = (Paging128) machine;
  }

}

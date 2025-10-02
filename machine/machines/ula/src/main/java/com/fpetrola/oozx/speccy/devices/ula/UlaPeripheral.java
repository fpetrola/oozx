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
 */package com.fpetrola.oozx.speccy.devices.ula;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/** The ULA as most machines reach it: any even port. */
@Singleton
public class UlaPeripheral extends AbstractPeripheral {
  private final UlaPortHandler port;

  @Inject
  public UlaPeripheral(UlaPortHandler port) {
    this(port, 0x0001, 0x0000);
  }

  protected UlaPeripheral(UlaPortHandler port, int mask, int value) {
    super(List.of(Wired.at(mask, value, port)));
    this.port = port;
  }

  public void activate(SpectrumMachine machine) {
    port.on(machine);
  }

  public void deactivate() {
    port.off();
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.fullyDecodesPorts();
  }
}

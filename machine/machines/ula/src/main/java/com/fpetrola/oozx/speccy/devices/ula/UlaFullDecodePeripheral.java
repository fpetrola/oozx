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
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** The same ULA on a machine that decodes every bit of the address: only 0xFE itself. */
@Singleton
public class UlaFullDecodePeripheral extends UlaPeripheral {
  @Inject
  public UlaFullDecodePeripheral(UlaPortHandler port) {
    super(port, 0x00ff, 0x00fe);
  }

  public boolean fitsOn(SpectrumMachine machine) {
    return machine.fullyDecodesPorts();
  }
}

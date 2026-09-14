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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * The second chip's register port, on the machine that has a second chip. One byte names a
 * register in its top half and gives it a value in the bottom, and the machine says what that
 * means - here, only the first of those registers, which is how fast to run.
 */
@Singleton
public class ChloeUla2Peripheral extends AbstractPeripheral {
  @Inject
  public ChloeUla2Peripheral() {
    super(List.of());
    ports(Wired.at(0xffff, 0x8e3b, new DefaultPortHandler(false, true) {
      @Override
      public void write(int port, byte value) {
        if (on instanceof Chloe280Se chloe) chloe.ula2Write(value);
      }
    }));
  }

  private SpectrumMachine on;

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine instanceof Chloe280Se;
  }
}

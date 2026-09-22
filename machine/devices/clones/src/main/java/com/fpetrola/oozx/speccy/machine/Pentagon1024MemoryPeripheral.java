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

/** The second paging port of the machine that needs one, and of nothing else. */
@Singleton
public class Pentagon1024MemoryPeripheral extends AbstractPeripheral {
  private Pentagon1024 machine;

  @Inject
  public Pentagon1024MemoryPeripheral() {
    super(List.of());
    ports(Wired.at(0xf008, 0xe000, new DefaultPortHandler(false, true) {
      @Override
      public void write(int port, byte value) {
        if (machine != null) machine.secondPortWrite(value);
      }
    }));
  }

  @Override
  public void activate(SpectrumMachine on) {
    machine = on instanceof Pentagon1024 one ? one : null;
  }

  @Override
  public void deactivate() {
    machine = null;
  }

  @Override
  public boolean fitsOn(SpectrumMachine on) {
    return on instanceof Pentagon1024;
  }
}

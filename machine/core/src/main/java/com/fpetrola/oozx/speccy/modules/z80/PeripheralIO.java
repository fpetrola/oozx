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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.ports.ContendedPortBus;
import com.fpetrola.oozx.speccy.ports.PortBus;
import com.fpetrola.z80.cpu.IO;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The processor's ports, wired to the peripheral bus.
 * <p>
 * This is what the Z80 used to build for itself inside createIO, which left no way to put
 * anything else on the ports. It is a class rather than a lambda so that it can be replaced —
 * an RZX recording plays back what each IN returned instead of asking the hardware — and it is
 * left open so it can be wrapped: recording is the same thing observed rather than substituted.
 * <p>
 * It holds no state of its own, which is why moving from one built per init path to a single
 * injected instance changes nothing.
 */
@Singleton
public class PeripheralIO implements IO {

  private final PortBus peripherals;

  @Inject
  public PeripheralIO(ContendedPortBus peripherals) {
    this.peripherals = peripherals;
  }

  @Override
  public int in(int port) {
    return peripherals.read(port);
  }

  @Override
  public void out(int port, int value) {
    peripherals.write(port, (byte) value);
  }
}

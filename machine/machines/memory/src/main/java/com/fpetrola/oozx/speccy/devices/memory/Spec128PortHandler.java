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

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.machine.Paging128;

class Spec128PortHandler extends DefaultPortHandler {
  private final Supplier<? extends Paging128> machine;

  public Spec128PortHandler(Supplier<? extends Paging128> machine) {
    super(false, true);
    this.machine = machine;
  }

  public void write(int port, byte value) {
    machine.get().memoryPortWrite(port, value);
  }

  /**
   * This pager decodes neither /RD nor /WR, so reading its port latches the video data the ULA
   * leaves floating and pages the machine - which games use. A machine that drives its bus, the
   * +3 family and the Pentagon, leaves nothing there to latch.
   */
  public boolean ignoresReadWriteLine() {
    return machine.get().hasFloatingBus();
  }
}

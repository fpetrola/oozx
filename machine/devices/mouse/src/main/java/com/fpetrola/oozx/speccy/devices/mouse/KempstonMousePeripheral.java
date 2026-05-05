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
package com.fpetrola.oozx.speccy.devices.mouse;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.util.List;

/**
 * A Kempston Mouse hanging off the expansion port, which is a thing no Spectrum came with.
 * <p>
 * Three ports, all read, nothing written, no ROM of its own: this is about the smallest a
 * peripheral gets, and what it adds is out of all proportion to that - a machine whose software
 * was written for a keyboard suddenly being pointed at.
 * <p>
 * The port patterns are given as a mask and the
 * value the masked port must equal, so the buttons answer at 0xFADF, the horizontal count at
 * 0xFBDF and the vertical at 0xFFDF, along with everything else that decodes the same.
 */
@com.google.inject.Singleton
public class KempstonMousePeripheral extends PluggablePeripheral {

  private final KempstonMouse mouse;

  @com.google.inject.Inject
  public KempstonMousePeripheral(KempstonMouse mouse) {
    super(List.of(
        Wired.at(0x0121, 0x0001, new KempstonMousePortHandler(mouse::buttons)),
        Wired.at(0x0521, 0x0101, new KempstonMousePortHandler(mouse::x)),
        Wired.at(0x0521, 0x0501, new KempstonMousePortHandler(mouse::y))));
    this.mouse = mouse;
  }

  public KempstonMouse mouse() {
    return mouse;
  }

  /** Unplugging puts the buttons back up, so nothing is left held down. */
  @Override
  public void plugIn(boolean connected) {
    super.plugIn(connected);
    if (!connected) {
      mouse.rest();
    }
  }

  /**
   * Any of them. It plugs into the expansion port and asks nothing of the machine behind it,
   * which is why one could be bought for a Spectrum somebody already owned.
   */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }
}

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
package com.fpetrola.oozx.speccy.devices.fuller;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fuller Box: the 128's sound chip before there was a 128, on ports 0x3f (which register)
 * and 0x5f (its value), and a joystick read on 0x7f, active low. Sold for the 48K.
 */
@Singleton
public class FullerPeripheral extends AyPeripheral implements Pluggable {

  private boolean pluggedIn;

  @Inject
  public FullerPeripheral(Sound sound, Z80Clock clock, Joystick joystick) {
    super(sound, clock, 0x00ff, 0x003f, 0x00ff, 0x005f, false);
    List<Wired> all = new ArrayList<>(List.of(getPorts()));
    all.add(Wired.at(0x00ff, 0x007f, new DefaultPortHandler(true, false) {
      public BusAnswer read(int port) {
        return joystick.fullerRead(port);
      }
    }));
    ports(all.toArray(new Wired[0]));
  }

  @Override
  public void plugIn(boolean connected) {
    pluggedIn = connected;
  }

  @Override
  public boolean isPluggedIn() {
    return pluggedIn;
  }

  @Override
  public boolean isWanted() {
    return pluggedIn;
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
  }
}

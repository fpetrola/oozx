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

package com.fpetrola.oozx.speccy.devices.melodik;


import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.z80.cpu.Z80Clock;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;

/**
 * A box with an AY in it, for a machine that has not got one.
 * <p>
 * The Melodik decodes the chip exactly as a 128 does, which is the point of it: music written for
 * a 128K plays on a 48K with one of these plugged in. So it is that same chip, and the only
 * difference is that a machine comes with one of those and somebody chose this.
 */
@com.google.inject.Singleton
public class MelodikPeripheral extends AyPeripheral {

  private boolean fitted;

  @com.google.inject.Inject
  public MelodikPeripheral(Sound sound, Z80Clock clock) {
    super(sound, clock, false);
  }

  @Override
  public boolean isWanted() {
    return fitted;
  }

  /** The point of the box: it goes where the machine has no chip of its own. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.hasOnBoard(AyPeripheral.class);
  }

  public boolean fitted() {
    return fitted;
  }

  public void setFitted(boolean fitted) {
    this.fitted = fitted;
  }
}

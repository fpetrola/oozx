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
package com.fpetrola.oozx.speccy.devices.specdrum;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.modules.sound.Dac;
import com.fpetrola.oozx.speccy.modules.sound.DacDevice;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/** The Cheetah SpecDrum: an eight-bit DAC on port 0xdf, the byte written being a signed level. */
@Singleton
public class SpecDrumPeripheral extends PluggablePeripheral implements DacDevice {

  public static final double FULL_SCALE = 128 * 128;

  private final Sound sound;
  private int volume;
  private Dac dac;

  @Inject
  public SpecDrumPeripheral(Sound sound, Z80Clock clock) {
    super(List.of());
    this.sound = sound;
    ports(Wired.at(0x00ff, 0x00df, new DefaultPortHandler(false, true) {
      public void write(int port, byte b) {
        if (dac != null) {
          dac.write(clock.getTStates(), ((b & 0xff) - 128) * 128);
        }
      }
    }));
  }

  /** A 48K or a 128: not the Amstrad machines, not a clone. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    dac = sound.add(new Dac(sound, volume));
  }

  @Override
  public void deactivate() {
    if (dac != null) {
      sound.remove(dac);
      dac = null;
    }
  }

  @Override
  public Dac dac() {
    return dac;
  }

  @Override
  public int volume() {
    return volume;
  }

  @Override
  public void setVolume(int percent) {
    volume = percent;
    if (dac != null) {
      sound.remove(dac);
      dac = sound.add(new Dac(sound, percent));
    }
  }

}

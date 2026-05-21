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
package com.fpetrola.oozx.speccy.devices.covox;

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

/**
 * The Covox: an eight-bit DAC on port 0xfb, the byte written being the level. It belongs to the
 * Pentagon, which is where it was sold, and answers 0xdd as well when it is there.
 */
@Singleton
public class CovoxPeripheral extends PluggablePeripheral implements DacDevice {

  public static final double FULL_SCALE = 255 * 128;

  private final Sound sound;
  private int volume;
  private Dac dac;

  @Inject
  public CovoxPeripheral(Sound sound, Z80Clock clock) {
    super(List.of());
    this.sound = sound;
    ports(port(0xfb, clock), port(0xdd, clock));
  }

  private Wired port(int value, Z80Clock clock) {
    return Wired.at(0x00ff, value, new DefaultPortHandler(false, true) {
      public void write(int port, byte b) {
        if (dac != null) {
          dac.write(clock.getTStates(), (b & 0xff) * 128);
        }
      }
    });
  }

  /** The Russian clones, which decode their ports fully. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.fullyDecodesPorts();
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

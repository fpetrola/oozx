/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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

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
package com.fpetrola.oozx.speccy.devices.specdrum;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.sound.Dac;
import com.fpetrola.oozx.speccy.sound.DacDevice;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/** The Cheetah SpecDrum: an eight-bit DAC on port 0xdf, the byte written being a signed level. */
@Singleton
public class SpecDrumPeripheral extends PluggablePeripheral implements DacDevice {

  public static final double FULL_SCALE = 128 * 128;

  private final Sound sound;
  private final Settings settings;
  private Dac dac;

  @Inject
  public SpecDrumPeripheral(Sound sound, Z80Clock clock, Settings settings) {
    super(List.of());
    this.sound = sound;
    this.settings = settings;
    ports(new DefaultPortHandler(0x00ff, 0x00df, false, true) {
      public void write(int port, byte b) {
        if (dac != null) {
          dac.write(clock.getTStates(), ((b & 0xff) - 128) * 128);
        }
      }
    });
  }

  /** A 48K or a 128: not the Amstrad machines, not a clone. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    dac = sound.add(new Dac(sound, settings.current.volumeSpecdrum));
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
    return settings.current.volumeSpecdrum;
  }

  @Override
  public void setVolume(int percent) {
    settings.current.volumeSpecdrum = percent;
    if (dac != null) {
      sound.remove(dac);
      dac = sound.add(new Dac(sound, percent));
    }
  }
}

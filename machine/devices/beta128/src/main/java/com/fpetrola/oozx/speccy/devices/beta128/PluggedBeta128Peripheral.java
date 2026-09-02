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
package com.fpetrola.oozx.speccy.devices.beta128;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The same board as the Pentagon's, bought and plugged into a 48K or a 128: it brings its own
 * TR-DOS ROM file, and it is there when its window is clipped on.
 */
@Singleton
public class PluggedBeta128Peripheral extends Beta128Peripheral {

  @Inject
  public PluggedBeta128Peripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                                  Machine machine, Joystick joystick) {
    super(memory, module, cpu, settings, events, machine, joystick);
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean isWanted() {
    return isPluggedIn();
  }

  @Override
  public String romName() {
    return settings.current.romBeta128;
  }

  @Override
  protected String defaultRomName() {
    return settings.defaults.romBeta128;
  }
}

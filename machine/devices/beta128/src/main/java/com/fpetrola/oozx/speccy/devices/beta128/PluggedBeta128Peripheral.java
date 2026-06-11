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

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.modules.input.Input;

/**
 * The same board as the Pentagon's, bought and plugged into a 48K or a 128: it brings its own
 * TR-DOS ROM file, and it is there when its window is clipped on.
 */
@Singleton
public class PluggedBeta128Peripheral extends Beta128Peripheral {

  @Inject
  public PluggedBeta128Peripheral(MemoryBus memory, Cpu cpu, Beta128Peripheral.TrDos trdos, Fdd.Limits floppy, Scheduler events,
                                  Machine machine, Joystick joystick, com.fpetrola.oozx.speccy.machine.Roms roms, Input.Setup input) {
    super(memory, cpu, trdos, floppy, events, machine, joystick, roms, input);
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean isWanted() {
    return isPluggedIn();
  }


}

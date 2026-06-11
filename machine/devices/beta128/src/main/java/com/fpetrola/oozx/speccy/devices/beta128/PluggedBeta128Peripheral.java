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

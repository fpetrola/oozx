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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.devices.scld.ScldPeripheral;
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

/**
 * The Timex the Portuguese sold: a 48K's memory and a 48K's ROM socket, with a different chip
 * doing the drawing. What the SCLD adds is a second display file and a choice of what a colour
 * covers, so the same bitmap can be coloured a line at a time instead of a cell at a time.
 */
@Singleton
public class Tc2048 extends Spec48 {
  @Inject
  public Tc2048(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Machine.Unit unit, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, unit, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(ScldPeripheral.class, TimexMemoryPeripheral.class);
  }

  /** The SCLD answers 0xf4 and 0xff as well as the ULA's own port. */
  @Override
  public boolean portFromUla(int port) {
    int low = port & 0xff;
    return low == 0xf4 || low == 0xfe || low == 0xff;
  }

  @Override
  public String shortName() {
    return "TC2048";
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String getName() {
    return "Timex TC2048";
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3500000, MachineTimings.TIMEX_SCLD_50HZ);

  @Override
  public MachineTimings getTimings() {
    return TIMINGS;
  }
}

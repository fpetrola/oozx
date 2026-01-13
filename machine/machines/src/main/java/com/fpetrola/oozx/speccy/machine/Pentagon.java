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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.devices.memory.Spec128MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * A 1991-era Pentagon 128, the Russian clone with an AY and TR-DOS built in.
 * <p>
 * It pages like a 128 and is timed like nothing else here: 320 lines of 224 clocks at 3.584MHz,
 * and not one address or port is contended. That last part is why Pentagon software runs fast and
 * why timing-exact 48K demos break on it.
 * <p>
 * Its Beta 128 is on the board: the third ROM is TR-DOS, and the WD1793 behind it reads a disk image.
 */
@Singleton
public class Pentagon extends Spec128 {

  @Inject
  public Pentagon(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                  Roms roms, Scheduler scheduler, Cpu cpu, Timer timer,
                  Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, Spec128MemoryPeripheral.class, Beta128Peripheral.class);
  }

  public boolean pagesThrough7ffd() {
    return true;
  }

  public boolean fullyDecodesPorts() {
    return true;
  }

  /** Pentagon contends nothing at all. */
  @Override
  protected Waits waits() {
    return Waits.NONE;
  }

  /** No memory contention and no floating-bus behaviour. */
  public boolean hasFloatingBus() {
    return false;
  }

  public String shortName() {
    return "Pentagon";
  }

  /** No snapshot model maps to Pentagon, so it cannot be selected by loading a snapshot. */
  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public int reset() {
    return doReset();
  }

  @Override
  protected boolean contendsMemory() {
    return false;
  }

  @Override
  protected void installPeripherals() {
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3584000, MachineTimings.PENTAGON);

  public MachineTimings getTimings() {
    return TIMINGS;
  }

  @Override
  public String getName() {
    return "Pentagon";
  }
}

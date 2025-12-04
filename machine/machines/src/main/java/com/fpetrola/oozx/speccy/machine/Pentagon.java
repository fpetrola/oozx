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

  /** A Pentagon's ULA holds nobody up. */
  @Override
  protected Waits waits() {
    return Waits.NONE;
  }

  /** A Pentagon has no contention and no floating bus. */
  public boolean hasFloatingBus() {
    return false;
  }

  public String shortName() {
    return "Pentagon";
  }

  /** No snapshot format here names a Pentagon, so it is never chosen by one. */
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

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
import com.fpetrola.oozx.speccy.devices.scld.ScldPeripheral;
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.ulaplus.UlaPlusPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Display;
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
 * The SE with the two things its designer added afterwards: sixty-four colours a program picks
 * itself, and a register that tells the machine to run faster than it was built to. The memory is
 * the SE's - both kinds of paging at once, with the cartridge slots holding RAM - and so is the ROM.
 */
@Singleton
public class Chloe280Se extends SpecSe {
  /** What the low half of the speed register means, in how many cycles fit where one fitted. */
  private static final int[] SPEEDS = {1, 1, 2, 1, 4, 1, 8, 1, 16, 1, 16, 1, 16, 1, 16, 1};

  private static final MachineTimings TIMINGS = new MachineTimings(3500000, MachineTimings.FERRANTI_5C_6C);

  @Inject
  public Chloe280Se(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                    Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, Spec128MemoryPeripheral.class, ScldPeripheral.class,
        TimexMemoryPeripheral.class, UlaPlusPeripheral.class, ChloeUla2Peripheral.class);
  }

  /**
   * A register named by the top half of a byte and given the bottom half. Only the first of them
   * is a register here: how many of the processor's cycles fit where one fitted when it was built.
   */
  public void ula2Write(byte value) {
    if ((value & 0xf0) != 0) return;
    runsTimesFaster(SPEEDS[value & 0x0f]);
  }

  /** Two of its pages are contended, where the SE contends every odd one. */
  @Override
  public int reset() {
    int result = super.reset();
    for (int page = 0; page < 9; page++) {
      banks.ram(page).contended = page == 5 || page == 7;
    }
    return result;
  }

  @Override
  public MachineTimings getTimings() {
    return atThisSpeed(TIMINGS);
  }

  @Override
  public String shortName() {
    return "Chloe280SE";
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String getName() {
    return "Chloe 280SE";
  }
}

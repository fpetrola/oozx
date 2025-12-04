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

import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fasterxml.jackson.annotation.JsonMerge;

@Singleton
public class Spec48 extends Spectrum {

  @Inject
  public Spec48(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Machine.Unit unit, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, scheduler, cpu, timer, peripherals, sound, roms);
    this.unit = unit;
  }

  private final Machine.Unit unit;

  // Initialize the Spectrum 48K speccyMachineInfo

  public String shortName() {
    return "48K";
  }

  // Reset the Spectrum 48K machine
  public MachineTypes snapshotModel() {
    return MachineTypes.SPECTRUM48K;
  }

  public int reset() {
    loadRom(0, 0x4000);

    peripherals.clear();
    peripherals.update();

    banks.show(banks.ram(5), display::screenWritten);

    commonDisplaySetup();

    return commonReset();
  }

  /** An issue 2 ULA's undriven bits follow the tape bit as well as the speaker's. */
  @Override
  protected int bitsThatLiftTheIdleValue() {
    return unit.issue2 ? 0x18 : 0x10;
  }

  // Set up common display configuration
  public void commonDisplaySetup() {
  }

  // Common reset for Spectrum 48K
  public int commonReset() {
    // 0x0000: ROM 0
    memory.slot(0x0000, banks.rom(0));
    // 0x4000: RAM 5, contended
    banks.ram(5).contended = true;
    memory.slot(0x4000, banks.ram(5));
    // 0x8000: RAM 2, not contended
    banks.ram(2).contended = false;
    memory.slot(0x8000, banks.ram(2));
    // 0xc000: RAM 0, not contended
    banks.ram(0).contended = false;
    memory.slot(0xc000, banks.ram(0));

    return 0;
  }

  // Map memory for Spectrum 48K
  public void memoryMap() {
    memory.slot(0x0000, banks.rom(0));
  }

  @Override
  public String getName() {
    return "Spectrum 48K";
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3500000, MachineTimings.FERRANTI_5C_6C);

  public MachineTimings getTimings() {
    return TIMINGS;
  }
}

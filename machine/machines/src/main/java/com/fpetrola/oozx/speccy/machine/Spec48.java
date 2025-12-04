/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

  public String shortName() {
    return "48K";
  }

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

  /** Issue-2 ULAs additionally float the tape bit onto the undriven bits, unlike issue 3. */
  @Override
  protected int bitsThatLiftTheIdleValue() {
    return unit.issue2 ? 0x18 : 0x10;
  }

  public void commonDisplaySetup() {
  }

  public int commonReset() {
    memory.slot(0x0000, banks.rom(0));
    banks.ram(5).contended = true;
    memory.slot(0x4000, banks.ram(5));
    banks.ram(2).contended = false;
    memory.slot(0x8000, banks.ram(2));
    banks.ram(0).contended = false;
    memory.slot(0xc000, banks.ram(0));

    return 0;
  }

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

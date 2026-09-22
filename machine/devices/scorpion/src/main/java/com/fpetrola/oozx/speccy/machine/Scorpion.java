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
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.memory.SpecPlus3MemoryPeripheral;
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
 * A clone with a quarter of a megabyte, paged through the two ports a +3 has but read differently:
 * the bit that puts RAM at the bottom puts page zero there and nothing else moves, one bit picks a
 * ROM of its own rather than one of a pair, and another is simply a fourth bit of the page number.
 */
@Singleton
public class Scorpion extends SpecPlus3 {
  /** The machine's own ROM, which is neither of the two a 128 chooses between. */
  private static final int ITS_OWN_ROM = 0x02;
  /** The fourth bit of the page number, which is what a quarter of a megabyte needs. */
  private static final int FOURTH_BIT = 0x10;

  private static final MachineTimings TIMINGS = new MachineTimings(3500000, MachineTimings.SCORPION);

  @Inject
  public Scorpion(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                  Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, SpecPlus3MemoryPeripheral.class, Beta128Peripheral.class);
  }

  @Override
  public int reset() {
    loadRom(0, 0x4000);
    loadRom(1, 0x4000);
    loadRom(2, 0x4000);
    commonReset(false);
    peripherals.clear();
    resetStep2();
    return 0;
  }

  /**
   * Sixteen pages, none of them contended. The bit that a +3 reads as the first of four all-RAM
   * layouts this machine reads as one thing only: page zero underneath, and the rest where it was.
   */
  @Override
  protected int pageAt(int slot) {
    if (slot == 0) return 0;
    if (slot != 3) return super.pageAt(slot);
    return ((paging.port1ffd() & FOURTH_BIT) >> 1) | (paging.port7ffd() & 0x07);
  }

  @Override
  protected int romAt() {
    return (paging.port1ffd() & ITS_OWN_ROM) != 0 ? 2 : (paging.port7ffd() & 0x10) >> 4;
  }

  @Override
  protected boolean contendsMemory() {
    return false;
  }

  @Override
  public boolean portFromUla(int port) {
    return false;
  }

  @Override
  public MachineTimings getTimings() {
    return TIMINGS;
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String shortName() {
    return "Scorpion";
  }

  @Override
  public String getName() {
    return "Scorpion ZS 256";
  }
}

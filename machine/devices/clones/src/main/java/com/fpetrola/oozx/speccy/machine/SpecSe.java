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
import com.fpetrola.oozx.speccy.devices.scld.ScldPeripheral;
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryPart;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
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
 * A 128 whose display chip is a Timex one and whose cartridge slots hold RAM rather than a
 * cartridge, so the machine has both kinds of paging at once and the two meet in one place: an odd
 * page at the top lets two more bits of the slot port reach into it.
 */
@Singleton
public class SpecSe extends Spec128 {
  private static final int PAGES = 9;
  private static final int TOP_HALVES = 0xc0;

  private final MemoryPart[] slot = new MemoryPart[8];
  private final MemoryPart[] behind = new MemoryPart[8];

  @Inject
  public SpecSe(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  /**
   * The sixteen eight K of the slots, made the first time this machine is switched on. Every
   * machine this build has is built when the emulator is, and one that is never selected has no
   * business holding a hundred and twenty-eight K nobody is going to read.
   */
  private void slotsExist() {
    if (slot[0] != null) return;
    for (int chunk = 0; chunk < 8; chunk++) {
      slot[chunk] = new Ram(0x2000);
      behind[chunk] = new Ram(0x2000);
    }
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, Spec128MemoryPeripheral.class,
        ScldPeripheral.class, TimexMemoryPeripheral.class);
  }

  @Override
  public boolean fullyDecodesPorts() {
    return true;
  }

  /** Nothing of the picture is ever left on its bus, so a port nobody answers reads as nothing. */
  @Override
  public boolean hasFloatingBus() {
    return false;
  }

  @Override
  public int reset() {
    int result = super.reset();
    slotsExist();
    for (int page = 0; page < PAGES; page++) {
      banks.ram(page).contended = (page & 1) != 0;
    }
    TimexMemoryPeripheral slots = (TimexMemoryPeripheral) peripherals.find(TimexMemoryPeripheral.class);
    if (slots != null) slots.carrying(slot, behind);
    return result;
  }

  /** Its own eight K above the screen is page 8, where a 128 has page 2. */
  @Override
  protected int pageAt(int slot) {
    return slot == 2 ? 8 : super.pageAt(slot);
  }

  /**
   * The two kinds of paging meet here. The 128's map goes down first and the slots cover it; then,
   * only while the page at the top is an odd one, the two bits that cover the screen's halves cover
   * the top two eight K as well.
   */
  @Override
  public void memoryMap() {
    super.memoryMap();
    TimexMemoryPeripheral slots = (TimexMemoryPeripheral) peripherals.find(TimexMemoryPeripheral.class);
    if (slots == null) return;
    boolean odd = (pageAt(3) & 1) != 0;
    slots.alsoCover(odd ? (slots.wanted() & 0x0c) << 4 & TOP_HALVES : 0);
  }

  @Override
  public String shortName() {
    return "SE";
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String getName() {
    return "Spectrum SE";
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3500000,
      new MachineTimings.Frame(new MachineTimings.Span(24, 128, 24, 48),
          new MachineTimings.Span(47, 192, 48, 25), 32, 14336));

  @Override
  public MachineTimings getTimings() {
    return TIMINGS;
  }
}

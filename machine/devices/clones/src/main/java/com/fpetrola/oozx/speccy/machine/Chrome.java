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

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

/**
 * A 128 with two more sixteen K and two more ROMs, and a second port of its own that says what to
 * do with them: put one of the two pages where the ROM is, put the other where the screen is,
 * choose between four ROMs instead of two, and run at twice the speed.
 * <p>
 * One bit of that port turns all of it off and leaves a 128 behind, which is the machine's own way
 * of running what will not have it.
 * <p>
 * It was sold with a +D inside. That interface is one this emulator has, but as a thing somebody
 * plugs in rather than a thing a machine is made of, so here it is plugged in rather than built in.
 */
@Singleton
public class Chrome extends Spec128 implements PagingPlus3 {
  /** One of the two pages that are not a 128's goes where the ROM is. */
  private static final int RAM_BELOW = 0x01;
  /** Which of the two it is, and the high bit of which of the four ROMs is read. */
  private static final int THE_SECOND_ONE = 0x02;
  /** The other of them goes where the screen is, and the screen goes on being shown from where it was. */
  private static final int NINE_AT_THE_SCREEN = 0x04;
  /** Twice the speed. */
  private static final int FASTER = 0x08;
  /** Everything of its own switched off, which leaves a 128. */
  private static final int JUST_A_128 = 0x20;

  private static final MachineTimings TIMINGS = new MachineTimings(3580000, MachineTimings.FERRANTI_7C);

  private byte second;

  @Inject
  public Chrome(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, SpecPlus3MemoryPeripheral.class);
  }

  @Override
  public boolean pagesThrough1ffd() {
    return true;
  }

  /** Whether the machine is being itself, which one bit of its own port decides. */
  private boolean itself() {
    return (second & JUST_A_128) == 0;
  }

  @Override
  public void memoryPort2Write(int port, byte b) {
    if (paging.locked()) return;
    second = b;
    runsTimesFaster(itself() && (second & FASTER) != 0 ? 2 : 1);
    memoryMap();
  }

  @Override
  public int reset() {
    second = 0;
    int result = super.reset();
    for (int page = 0; page < 10; page++) {
      banks.ram(page).contended = page == 2 || page == 5;
    }
    return result;
  }

  /**
   * Four ROMs where a 128 has two: the bit a 128 chooses with, and one of this machine's own above
   * it. The emulator these were read from takes that high bit from the bit beside the one its own
   * notes name, and the notes are what a machine was built from, so they are what is followed here.
   */
  @Override
  protected int romAt() {
    int low = (paging.port7ffd() & 0x10) >> 4;
    return itself() && (second & THE_SECOND_ONE) != 0 ? low + 2 : low;
  }

  /** Page eight or page nine underneath, and page nine where the screen is, while it is itself. */
  @Override
  protected int pageAt(int slot) {
    if (itself() && slot == 0 && (second & RAM_BELOW) != 0) {
      return (second & THE_SECOND_ONE) != 0 ? 9 : 8;
    }
    if (itself() && slot == 1 && (second & NINE_AT_THE_SCREEN) != 0) {
      return 9;
    }
    return super.pageAt(slot);
  }

  /** Its own port can put RAM where the ROM is, which a 128 has no way of asking for. */
  @Override
  public void memoryMap() {
    super.memoryMap();
    if (itself() && (second & RAM_BELOW) != 0) memory.slot(0x0000, banks.ram(pageAt(0)));
  }

  @Override
  public MachineTimings getTimings() {
    return atThisSpeed(TIMINGS);
  }

  @Override
  public String shortName() {
    return "Chrome";
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String getName() {
    return "Chrome";
  }
}

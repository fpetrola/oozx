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
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import java.util.Set;


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
import com.fasterxml.jackson.annotation.JsonMerge;

@Singleton
public class Spec128 extends Spectrum implements Paging128 {

  @Inject
  public Spec128(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, scheduler, cpu, timer, peripherals, sound, roms);
  }

  /** First Sinclair model with a sound chip and paging, added via port 0x7ffd. */
  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPeripheral.class, Spec128MemoryPeripheral.class);
  }

  public boolean pagesThrough7ffd() {
    return true;
  }

  public String shortName() {
    return "128K";
  }

  @Override
  public MachineTypes snapshotModel() {
    return MachineTypes.SPECTRUM128K;
  }

  @Override
  public int reset() {
    return doReset();
  }

  protected int doReset() {
    loadRom(0, 0x4000);
    loadRom(1, 0x4000);
    commonReset(contendsMemory());

    peripherals.clear();
    installPeripherals();
    peripherals.update();

//    spec48.commonDisplaySetup();

    return 0;
  }

  /** Sinclair 128-family machines contend odd RAM pages; overridden to false for the Pentagon. */
  protected boolean contendsMemory() {
    return true;
  }

  protected void installPeripherals() {
  }

  public int commonReset(boolean contention) {
    paging.reset();
    banks.show(banks.ram(5), display::screenWritten);

    // Loop covers 16 pages for clones with that much RAM (e.g. Scorpion); odd ones contend.
    for (int i = 0; i < 16; i++) {
      banks.ram(i).contended = (i & 1) != 0 && contention;
    }

    memoryMap();
    return 0;
  }

  /** Which page a slot holds. A machine with more memory than the port has bits says so here. */
  protected int pageAt(int slot) {
    return paging.page(slot);
  }

  /** Which ROM is at the bottom. A machine that reads the same bits differently says so here. */
  protected int romAt() {
    return paging.rom();
  }

  public void memoryPortWrite(int port, byte b) {
    if (paging.write7ffd(b)) memoryMap();
  }

  /** Applies the current paging state to the memory slots, shared by all 128-style pagers. */
  @Override
  public void memoryMap() {
    if (banks.shown() != banks.ram(paging.screen())) {
      display.screenChanging();
      banks.show(banks.ram(paging.screen()), display::screenWritten);
    }
    memory.slot(0x0000, paging.special() ? banks.ram(pageAt(0)) : banks.rom(romAt()));
    for (int slot = 1; slot < 4; slot++) {
      memory.slot(slot << 14, banks.ram(pageAt(slot)));
    }
  }

  @Override
  public String getName() {
    return "Spectrum 128K";
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3546900, MachineTimings.FERRANTI_7C);

  public MachineTimings getTimings() {
    return TIMINGS;
  }

  /**
   * The 128K's two ROMs, editor and 48-BASIC-compatible, in the hardware's own socket numbering.
   * Subclasses with different ROM sets reuse these same fields. Config merges into a socket
   * rather than replacing it, so naming one file's ROM does not lose the shipped default.
   */
}

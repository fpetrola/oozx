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

  /** The 128 is the first with a sound chip and a pager, through port 0x7ffd. */
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

  /** Every Sinclair 128 contends its odd RAM pages; the Pentagon contends nothing. */
  protected boolean contendsMemory() {
    return true;
  }

  protected void installPeripherals() {
  }

  public int commonReset(boolean contention) {
    paging.reset();
    banks.show(banks.ram(5), display::screenWritten);

    // Odd pages contended on the 128K/+2; loop up to 16 for Scorpion's 256Kb RAM
    for (int i = 0; i < 16; i++) {
      banks.ram(i).contended = (i & 1) != 0 && contention;
    }

    memoryMap();
    return 0;
  }

  // Write to the 128K memory port (0x7FFD)
  public void memoryPortWrite(int port, byte b) {
    if (paging.write7ffd(b)) memoryMap();
  }

  /** The slots as the paging says they are, for the 128 and every machine that pages like it. */
  @Override
  public void memoryMap() {
    if (banks.shown() != banks.ram(paging.screen())) {
      display.screenChanging();
      banks.show(banks.ram(paging.screen()), display::screenWritten);
    }
    memory.slot(0x0000, paging.special() ? banks.ram(paging.page(0)) : banks.rom(paging.rom()));
    for (int slot = 1; slot < 4; slot++) {
      memory.slot(slot << 14, banks.ram(paging.page(slot)));
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
   * The 128K: the editor, and the 48K BASIC it pages in over it, in the two sockets the hardware
   * numbers them by.
   * <p>
   * The machines that are a 128K with other ROMs in it extend this, as those machines extend the
   * 128K itself, so each one is handed its own pair through the same fields. A socket is merged
   * rather than replaced when the file is read, so one that names another file keeps knowing which
   * ROM the machine shipped with.
   */
}

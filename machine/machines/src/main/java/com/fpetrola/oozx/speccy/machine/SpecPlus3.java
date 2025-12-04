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


import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;
import com.fpetrola.oozx.speccy.devices.memory.SpecPlus3MemoryPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
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
import com.fpetrola.oozx.speccy.peripherals.*;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fasterxml.jackson.annotation.JsonMerge;

@Singleton
public class SpecPlus3 extends Spec128 implements PagingPlus3 {
  @Inject
  public SpecPlus3(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
    specplus3MenuItems();
  }

  public boolean portFromUla(int port) {
    // Amstrad ULA contends nothing.
    return false;
  }

  /** The +3 pages through both 0x1ffd and 0x7ffd, so its memory peripheral handles both ports. */
  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPlus3Peripheral.class, SpecPlus3MemoryPeripheral.class, Upd765Peripheral.class);
  }

  public boolean pagesThrough7ffd() {
    return true;
  }

  public boolean pagesThrough1ffd() {
    return true;
  }

  /** Amstrad ULA never floats a bit; bit 6 always reads low regardless of the last write. */
  @Override
  protected int bitsThatLiftTheIdleValue() {
    return 0;
  }

  /** Amstrad wait pattern: 1, 0, then 7 down to 2, starting 4 T-states before the first pixel; skipped when no memory is requested. */
  @Override
  protected Waits waits() {
    return Waits.ONE_THEN_SEVEN_DOWN_TO_TWO;
  }

  @Override
  protected Waits waitsWithoutMreq() {
    return Waits.NONE;
  }

  /** +3-family machines actively drive the bus, so no floating video value ever appears. */
  public boolean hasFloatingBus() {
    return false;
  }

  public String shortName() {
    return "+3";
  }

  public MachineTypes snapshotModel() {
    return MachineTypes.SPECTRUMPLUS3;
  }

  public int reset() {
    resetPlus3();

    resetStep2();

    return 0;
  }

  protected void resetStep2() {
    peripherals.update();
    specplus3MenuItems();
//    spec48.commonDisplaySetup();
  }

  public int plus2aCommonReset() {
    paging.reset();
    banks.show(banks.ram(5), display::screenWritten);

    // Pages 4-7 are the contended half of a +2A/+3's RAM.
    for (int i = 0; i < 8; i++) {
      banks.ram(i).contended = i >= 4;
    }

    memoryMap();
    return 0;
  }

  public void memoryPort2Write(int port, byte b) {
    if (paging.write1ffd(b)) memoryMap();
  }

  public void specplus3MenuItems() {
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_A], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_B], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
  }

  public String getName() {
    return "Spectrum Plus 3";
  }

  protected void resetPlus3() {
    loadRom(0, 0x4000);
    loadRom(1, 0x4000);
    loadRom(2, 0x4000);
    loadRom(3, 0x4000);

    plus2aCommonReset();

    peripherals.clear();
  }

  private static final MachineTimings TIMINGS = new MachineTimings(3546900, MachineTimings.AMSTRAD_ASIC);

  public MachineTimings getTimings() {
    return TIMINGS;
  }

  /** 128K plus two extra ROMs and a disk controller with one copy-protection accommodation;
   * +3 variants with different ROM sets subclass this. */
}

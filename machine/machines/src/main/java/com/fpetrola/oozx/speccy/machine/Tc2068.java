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

import com.fpetrola.oozx.speccy.devices.ay.AyTimexPeripheral;
import com.fpetrola.oozx.speccy.devices.scld.ScldPeripheral;
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryPart;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
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
 * The Timex with a sound chip and a cartridge slot: a TC2048 with an AY on two ports of its own,
 * eight K of ROM behind the slot, and nothing in the slot until somebody puts a cartridge there.
 */
@Singleton
public class Tc2068 extends Tc2048 {
  private final MemoryPart[] slot = new MemoryPart[8];
  private final MemoryPart[] behind = new MemoryPart[8];

  @Inject
  public Tc2068(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Machine.Unit unit, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, unit, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(ScldPeripheral.class, TimexMemoryPeripheral.class, AyTimexPeripheral.class);
  }

  @Override
  public int reset() {
    int result = super.reset();
    Rom exrom = new Rom(0x2000, null);
    exrom.fill(roms.of(this, 1, 0x2000));
    for (int chunk = 0; chunk < 8; chunk++) {
      slot[chunk] = banks.absent();
      behind[chunk] = exrom;
    }
    TimexMemoryPeripheral slots = (TimexMemoryPeripheral) peripherals.find(TimexMemoryPeripheral.class);
    if (slots != null) slots.carrying(slot, behind);
    return result;
  }

  @Override
  public String shortName() {
    return "TC2068";
  }

  @Override
  public String getName() {
    return "Timex TC2068";
  }
}

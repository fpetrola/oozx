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


import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The one Sinclair sold first: the same machine as the 48K with three quarters of its memory
 * missing, so everything above 0x7fff is bus and nothing else.
 */
@Singleton
public class Spec16 extends Spec48 {
  @Inject
  public Spec16(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Machine.Unit unit, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, unit, roms, scheduler, cpu, timer, sound);
  }

  @Override
  public String shortName() {
    return "16K";
  }

  @Override
  public MachineTypes snapshotModel() {
    return MachineTypes.SPECTRUM16K;
  }

  @Override
  public String getName() {
    return "Spectrum 16K";
  }

  @Override
  public int commonReset() {
    memory.slot(0x0000, banks.rom(0));
    banks.ram(5).contended = true;
    memory.slot(0x4000, banks.ram(5));
    memory.slot(0x8000, banks.absent());
    memory.slot(0xc000, banks.absent());
    return 0;
  }
}

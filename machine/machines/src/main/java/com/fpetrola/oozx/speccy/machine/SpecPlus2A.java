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

@Singleton
public class SpecPlus2A extends SpecPlus3 {
  @Inject
  public SpecPlus2A(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  /** A +3 without the floppy: same paging, no drive. */
  @Override
  public Set<Class<? extends Peripheral>> onBoard() {
    return Set.of(AyPlus3Peripheral.class, SpecPlus3MemoryPeripheral.class);
  }

  public String shortName() {
    return "+2A";
  }

  public MachineTypes snapshotModel() {
    return MachineTypes.SPECTRUMPLUS2A;
  }

  public int reset() {

    resetPlus3();

    peripherals.update();

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();

    return 0;
  }

  @Override
  protected void resetStep2() {
    peripherals.update();

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();
  }

  public String getName() {
    return "Spectrum Plus 2A";
  }
}

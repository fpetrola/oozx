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

import com.fpetrola.oozx.PeriphDelegate;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.IPeriph;

@Singleton
public class Spec48 extends Spectrum {

  @Inject
  public Spec48(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Sound sound) {
    super(memory, display, eventManager, cpu, timer, module, settings, new Spec48RamInfo(3), machinesPeriph, periph, sound);
  }

  // Initialize the Spectrum 48K speccyMachineInfo

  // Reset the Spectrum 48K machine
  public int reset() {
    int error = loadRom(0, settings.current.rom48, settings.defaults.rom48, 0x4000);
    if (error != 0) return error;

    periph.clear();
    machinesPeriph.machinesPeriph48();
    periph.update();

    Beta.builtin = false;

    memory.currentScreen = 5;
    memory.screenMask = 0xffff;

    commonDisplaySetup();

    return commonReset();
  }

  // Set up common display configuration
  public void commonDisplaySetup() {
  }

  // Common reset for Spectrum 48K
  public int commonReset() {
    // 0x0000: ROM 0
    memory.map16k(0x0000, memory.mapRom, 0);
    // 0x4000: RAM 5, contended
    memory.ramSet16kContention(5, true);
    memory.map16k(0x4000, memory.mapRam, 5);
    // 0x8000: RAM 2, not contended
    memory.ramSet16kContention(2, false);
    memory.map16k(0x8000, memory.mapRam, 2);
    // 0xc000: RAM 0, not contended
    memory.ramSet16kContention(0, false);
    memory.map16k(0xc000, memory.mapRam, 0);

    return 0;
  }

  // Map memory for Spectrum 48K
  public void memoryMap() {
    memory.map16k(0x0000, memory.mapRom, 0);
    memory.romcsMap(ramInfo);
  }

  public int unattachedPort(int port) {
    return spectrumUnattachedPort();
  }

  @Override
  public String getName() {
    return "Spectrum 48K";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3500000, 0, TimingsHandler.FERRANTI_5C_6C);
  }
}
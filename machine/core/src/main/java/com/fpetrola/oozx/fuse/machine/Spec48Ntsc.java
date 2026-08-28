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

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.Sound;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.modules.z80.Cpu;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;

public class Spec48Ntsc extends Spec48 {
  public Spec48Ntsc(Memory memory, Display display, MachinesPeriph machinesPeriph, IPeriph periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Sound sound) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, cpu, timer, module, sound);
    init();
  }

  public void init() {
  }

  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = loadRom(0, settings.current.rom48, settings.defaults.rom48, 0x4000);
    if (error != 0) return error;

    // Limpiar y configurar periféricos 48K
    periph.clear();
    machinesPeriph.machinesPeriph48();
    periph.update();

    // Pantalla en RAM 5
    memory.currentScreen = 5;
    memory.screenMask = 0xFFFF;

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();

    // Reset común de 48K
    return commonReset();
  }

  public int unattachedPort(int port) {
    return spectrumUnattachedPort();
  }

  public String getName() {
    return "Sinclair Spectrum 48K (NTSC)";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3579545, 1789772, TimingsHandler.FERRANTI_60HZ); // 3.579545 MHz
  }
}
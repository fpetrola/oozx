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

import com.fpetrola.oozx.MachineCapability;

import java.util.Set;

import static com.fpetrola.oozx.MachineCapability.*;

import com.fpetrola.oozx.PeriphDelegate;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.emulation.helpers.machine.MachineTypes;

@Singleton
public class Spec48Ntsc extends Spec48 {
  @Inject
  public Spec48Ntsc(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Sound sound, UserInterface userInterface) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, cpu, timer, module, sound, userInterface);
    init();
  }

  public void init() {
  }

  @Override
  public Set<MachineCapability> getCapabilities() {
    return Set.of(NTSC);
  }

  public String shortName() {
    return "48K_NTSC";
  }

  /** A snapshot does not say PAL or NTSC: a 48K one loads into the 48K. */
  public MachineTypes snapshotModel() {
    return null;
  }

  public int reset() {
    // Cargar ROM 0 (0x0000-0x3FFF)
    loadRom(0, settings.current.rom48, settings.defaults.rom48, 0x4000);

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
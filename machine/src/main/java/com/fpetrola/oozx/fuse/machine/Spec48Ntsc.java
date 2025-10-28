/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;

public class Spec48Ntsc extends AbstractSpectrumMachine {

  private Memory memory;
  private Display display;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private IPeriph periph;

  public Spec48Ntsc(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, IPeriph periph) {
    super(display, machine);
    this.memory = memory;
    this.display = display;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.periph = periph;
    this.ramInfo = new Spec48NtscRamInfo(3);
    init();
  }

  private void init() {
  }

  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = machine.loadRom(0, Settings.current.rom48, Settings.defaults.rom48, 0x4000);
    if (error != 0) return error;

    // Limpiar y configurar periféricos 48K
    periph.clear();
    machinesPeriph.machinesPeriph48();
    periph.update();

    // Pantalla en RAM 5
    memory.currentScreen = 5;
    memory.screenMask = 0xFFFF;

    // Configurar pantalla como en 48K
    spec48.commonDisplaySetup();

    // Reset común de 48K
    return spec48.commonReset();
  }

  private class Spec48NtscRamInfo extends RamInfo {
    public Spec48NtscRamInfo(int validPages) {
      this.validPages = validPages;
    }

    public boolean portFromUla(int port) {
      return spec48.portFromUla(port);
    }

    public int contendDelay(long time) {
      return spectrum.contendDelay65432100(time);
    }

    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelay65432100(time);
    }
  }

  public int unattachedPort(int port) {
    return spectrum.spectrumUnattachedPort();
  }

  public void memoryMap() {
    spec48.memoryMap();
  }

  public String getName() {
    return "Sinclair Spectrum 48K (NTSC)";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3579545, 1789772, TimingsHandler.FERRANTI_60HZ); // 3.579545 MHz
  }
}
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

public class SpecPlus2A extends AbstractSpectrumMachine {

  private Memory memory;
  private Display display;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private IPeriph periph;
  private SpecPlus3 specPlus3;

  public SpecPlus2A(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, IPeriph periph, SpecPlus3 specPlus3) {
    super(display, machine);
    this.memory = memory;
    this.display = display;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.periph = periph;
    this.specPlus3 = specPlus3;
    this.ramInfo = new SpecPlus2ARamInfo(8);
    init();
  }

  // ===================================================================
  // specplus2a_init() – Configura la máquina +2A
  // ===================================================================
  private void init() {
  }

  // ===================================================================
  // reset() – Reinicia la máquina +2A
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = machine.loadRom(0, Settings.current.romPlus2a0, Settings.defaults.romPlus2a0, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = machine.loadRom(1, Settings.current.romPlus2a1, Settings.defaults.romPlus2a1, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 2 (0x8000-0xBFFF)
    error = machine.loadRom(2, Settings.current.romPlus2a2, Settings.defaults.romPlus2a2, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 3 (0xC000-0xFFFF)
    error = machine.loadRom(3, Settings.current.romPlus2a3, Settings.defaults.romPlus2a3, 0x4000);
    if (error != 0) return error;

    // Reset común de +2A/+3
    error = specPlus3.plus2aCommonReset();
    if (error != 0) return error;

    // Limpiar y configurar periféricos +3
    periph.clear();
    machinesPeriph.machinesPeriphPlus3();
    periph.update();

    // Configurar pantalla como en 48K
    spec48.commonDisplaySetup();

    return 0;
  }

  // ===================================================================
  // RamInfo para +2A
  // ===================================================================
  private class SpecPlus2ARamInfo extends RamInfo {
    public SpecPlus2ARamInfo(int validPages) {
      this.validPages = validPages;
    }

    @Override
    public boolean portFromUla(int port) {
      return specPlus3.portFromUla(port);
    }

    @Override
    public int contendDelay(long time) {
      return spectrum.contendDelay76543210(time);
    }

    @Override
    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelayNone(time);
    }
  }

  // ===================================================================
  // unattachedPort() – delegado al machine
  // ===================================================================
  @Override
  public int unattachedPort(int port) {
    return spectrum.unattachedPortAmstrad(port); // Simula function pointer
  }

  @Override
  public String getName() {
    return "Spectrum Plus 2A";
  }

  @Override
  public void memoryMap() {
    specPlus3.memoryMap();
  }

  // ===================================================================
  // getBaseTiming()
  // ===================================================================
  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }
}
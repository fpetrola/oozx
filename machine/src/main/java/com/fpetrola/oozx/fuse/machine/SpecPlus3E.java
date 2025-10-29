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
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class SpecPlus3E extends AbstractSpectrumMachine {

  private Memory memory;
  private Display display;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private IPeriph periph;
  private SpecPlus3 specPlus3;

  public SpecPlus3E(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, IPeriph periph, SpecPlus3 specPlus3, Settings settings) {
    super(display, machine, settings);
    this.memory = memory;
    this.display = display;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.periph = periph;
    this.specPlus3 = specPlus3;
    this.ramInfo = new SpecPlus3ERamInfo(8);
    init();
  }

  // ===================================================================
  // specplus3e_init() – Configuración mínima (delegada a reset)
  // ===================================================================
  private void init() {
    // Configuración en reset() según tu estilo
  }

  // ===================================================================
  // reset() – Reinicia la máquina +3e
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = machine.loadRom(0, settings.current.romPlus3e0, settings.defaults.romPlus3e0, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = machine.loadRom(1, settings.current.romPlus3e1, settings.defaults.romPlus3e1, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 2 (0x8000-0xBFFF)
    error = machine.loadRom(2, settings.current.romPlus3e2, settings.defaults.romPlus3e2, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 3 (0xC000-0xFFFF)
    error = machine.loadRom(3, settings.current.romPlus3e3, settings.defaults.romPlus3e3, 0x4000);
    if (error != 0) return error;

    // Reset común de +2A/+3
    error = specPlus3.plus2aCommonReset();
    if (error != 0) return error;

    // Limpiar y configurar periféricos +3
    periph.clear();
    machinesPeriph.machinesPeriphPlus3();

    // FDC siempre presente
    periph.setPresent(Periph.Type.UPD765, Periph.Present.ALWAYS);

    periph.update();

    // Reset FDC y menús
    specPlus3.specplus3765Reset();
    specPlus3.specplus3MenuItems();

    // Configurar pantalla como en 48K
    spec48.commonDisplaySetup();

    return 0;
  }

  // ===================================================================
  // RamInfo para +3e
  // ===================================================================
  private class SpecPlus3ERamInfo extends RamInfo {
    public SpecPlus3ERamInfo(int validPages) {
      this.validPages = validPages;
    }

    public boolean portFromUla(int port) {
      return specPlus3.portFromUla(port);
    }

    public int contendDelay(long time) {
      return spectrum.contendDelay76543210(time);
    }

    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelayNone(time);
    }
  }

  public int unattachedPort(int port) {
    return spectrum.unattachedPortAmstrad(port);
  }

  public void memoryMap() {
    specPlus3.memoryMap();
  }

  public void shutdown() {
    specPlus3.shutdown();
  }

  public String getName() {
    return "Amstrad Spectrum +3e";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }
}
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
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;

import java.util.function.Supplier;

public class SpecPlus2A extends SpecPlus3 {
  private MachinesPeriph machinesPeriph;
  private IPeriph periph;
  private SpecPlus3 specPlus3;

  public SpecPlus2A(Memory memory, Display display, MachinesPeriph machinesPeriph, IPeriph periph, SpecPlus3 specPlus3, Settings settings, EventManager eventManager, Z80 z80, RAMHolder ramHolder, Supplier<SpectrumMachine> fuseMachineInfoSupplier, Timer timer, Module module) {
    super(memory, display, machinesPeriph, periph, settings, specPlus3.fdd, specPlus3.uPDFdc, eventManager, z80, ramHolder, fuseMachineInfoSupplier, timer, module);
    ramInfo= new SpecPlus2ARamInfo(8, this);
    this.machinesPeriph = machinesPeriph;
    this.periph = periph;
    this.specPlus3 = specPlus3;
    init();
  }

  // ===================================================================
  // reset() – Reinicia la máquina +2A
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = loadRom(0, settings.current.romPlus2a0, settings.defaults.romPlus2a0, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = loadRom(1, settings.current.romPlus2a1, settings.defaults.romPlus2a1, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 2 (0x8000-0xBFFF)
    error = loadRom(2, settings.current.romPlus2a2, settings.defaults.romPlus2a2, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 3 (0xC000-0xFFFF)
    error = loadRom(3, settings.current.romPlus2a3, settings.defaults.romPlus2a3, 0x4000);
    if (error != 0) return error;

    // Reset común de +2A/+3
    error = specPlus3.plus2aCommonReset();
    if (error != 0) return error;

    // Limpiar y configurar periféricos +3
    periph.clear();
    machinesPeriph.machinesPeriphPlus3();
    periph.update();

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();

    return 0;
  }

  // ===================================================================
  // RamInfo para +2A
  // ===================================================================
  private static class SpecPlus2ARamInfo extends RamInfo {
    private Spectrum spectrum;

    public SpecPlus2ARamInfo(int validPages, Spectrum spectrum) {
      this.spectrum = spectrum;
      this.validPages = validPages;
    }

    @Override
    public boolean portFromUla(int port) {
      return false;
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
    return unattachedPortAmstrad(port); // Simula function pointer
  }

  @Override
  public String getName() {
    return "Spectrum Plus 2A";
  }

  @Override
  public void memoryMap() {
    super.memoryMap();
  }

  // ===================================================================
  // getBaseTiming()
  // ===================================================================
  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }
}
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
import com.fpetrola.oozx.fuse.peripherals.*;

public class SpecPlus3E extends SpecPlus3 {

  private MachinesPeriph machinesPeriph;
  private IPeriph periph;

  public SpecPlus3E(Memory memory, Display display, MachinesPeriph machinesPeriph, IPeriph periph, Settings settings, EventManager eventManager, Z80 z80, Timer timer, Module module, Fdd fdd1, UPDFdc uPDFdc1) {
    super(memory, display, machinesPeriph, periph, settings, fdd1, uPDFdc1, eventManager, z80, timer, module);
    this.machinesPeriph = machinesPeriph;
    this.periph = periph;
    init();
  }

  // ===================================================================
  // reset() – Reinicia la máquina +3e
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = loadRom(0, settings.current.romPlus3e0, settings.defaults.romPlus3e0, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = loadRom(1, settings.current.romPlus3e1, settings.defaults.romPlus3e1, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 2 (0x8000-0xBFFF)
    error = loadRom(2, settings.current.romPlus3e2, settings.defaults.romPlus3e2, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 3 (0xC000-0xFFFF)
    error = loadRom(3, settings.current.romPlus3e3, settings.defaults.romPlus3e3, 0x4000);
    if (error != 0) return error;

    // Reset común de +2A/+3
    error = plus2aCommonReset();
    if (error != 0) return error;

    // Limpiar y configurar periféricos +3
    periph.clear();
    machinesPeriph.machinesPeriphPlus3();

    // FDC siempre presente
    periph.setPresent(Periph.Type.UPD765, Periph.Present.ALWAYS);

    periph.update();

    // Reset FDC y menús
    specplus3765Reset();
    specplus3MenuItems();

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();

    return 0;
  }

  public int unattachedPort(int port) {
    return unattachedPortAmstrad(port);
  }

  public void memoryMap() {
    super.memoryMap();
  }

  public void shutdown() {
    super.shutdown();
  }

  public String getName() {
    return "Amstrad Spectrum +3e";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }
}
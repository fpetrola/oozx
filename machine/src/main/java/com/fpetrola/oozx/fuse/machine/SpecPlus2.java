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
import com.fpetrola.oozx.fuse.peripherals.Spec128MemoryPeripheral;

import java.util.function.Supplier;

public class SpecPlus2 extends Spec128 {
  private MachinesPeriph machinesPeriph;
  private Spec128 spec128;
  private IPeriph periph;

  public SpecPlus2(Memory memory, Display display, MachinesPeriph machinesPeriph, Spec128 spec128, IPeriph periph, Settings settings, EventManager eventManager, Z80 z80, RAMHolder ramHolder, Supplier<SpectrumMachine> fuseMachineInfoSupplier, Timer timer, Module module) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, z80, ramHolder, fuseMachineInfoSupplier, timer, module);
    ramInfo = new SpecPlus2RamInfo(8, this);
    this.machinesPeriph = machinesPeriph;
    this.spec128 = spec128;
    this.periph = periph;
    init();
  }

  // ===================================================================
  // specplus2_init() – Configura la máquina +2
  // ===================================================================
  public void init() {
    periph.register(new Spec128MemoryPeripheral(this));
  }

  // ===================================================================
  // reset() – Reinicia la máquina +2
  // ===================================================================
  public int reset() {
    int error;

    // Cargar ROM 0 (0x0000-0x3FFF)
    error = loadRom(0, settings.current.romPlus20, settings.defaults.romPlus20, 0x4000);
    if (error != 0) return error;

    // Cargar ROM 1 (0x4000-0x7FFF)
    error = loadRom(1, settings.current.romPlus21, settings.defaults.romPlus21, 0x4000);
    if (error != 0) return error;

    // Reset común de 128K (con RAM lock = 1)
    error = spec128.commonReset(true);
    if (error != 0) return error;

    // Limpiar y configurar periféricos 128K
    periph.clear();
    machinesPeriph.machinesPeriph128();
    periph.update();

    // +2 no tiene Beta integrado
    Beta.builtin = false;

    // Configurar pantalla como en 48K
//    spec48.commonDisplaySetup();

    return 0;
  }

  @Override
  public void memoryMap() {
    super.memoryMap();
  }

  // ===================================================================
  // RamInfo para +2
  // ===================================================================
  private static class SpecPlus2RamInfo extends RamInfo {
    private Spectrum spectrum;

    public SpecPlus2RamInfo(int validPages, Spectrum spectrum) {
      this.spectrum = spectrum;
      this.validPages = validPages;
    }

    @Override
    public boolean portFromUla(int port) {
      return Spec48.portFromUlaStatic(port);
    }

    @Override
    public int contendDelay(long time) {
      return spectrum.contendDelay65432100(time);
    }

    @Override
    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelay65432100(time);
    }
  }

  // ===================================================================
  // unattachedPort()
  // ===================================================================
  @Override
  public int unattachedPort(int port) {
    return spectrumUnattachedPort();
  }

  @Override
  public String getName() {
    return "Spectrum Plus 2";
  }

  // ===================================================================
  // getBaseTiming()
  // ===================================================================
  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.FERRANTI_7C);
  }
}
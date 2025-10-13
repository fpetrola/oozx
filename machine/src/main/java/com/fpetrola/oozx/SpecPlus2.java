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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class SpecPlus2 extends AbstractSpectrumMachine implements SpectrumMachine {
  private Machine machine1;
  private MachinesPeriph machinesPeriph;
  private Spec48 spec48;
  private Spec128 spec128;
  private Spectrum spectrum;
  private Periph periph;

  public SpecPlus2(Display display, Machine machine, MachinesPeriph machinesPeriph, Spec48 spec48, Spec128 spec128, Spectrum spectrum, Periph periph) {
    super(display);
    this.machine1 = machine;
    this.machinesPeriph = machinesPeriph;
    this.spec48 = spec48;
    this.spec128 = spec128;
    this.spectrum = spectrum;
    this.periph = periph;
    init();
  }

  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return null;
  }

  // Initialize the Spectrum +2 machine
  @Override
  public SpectrumMachine init() {
    this.machine = Libspectrum.Machine.PLUS2;
    this.timex = false;
    this.ramInfo = new SpecPlus2RamInfo(8);

    return this;
  }

  // Reset the Spectrum +2 machine
  public int reset() {
    int error;

    error = machine1.loadRom(0, Settings.current.romPlus20, Settings.defaults.romPlus20, 0x4000);
    if (error != 0) return error;
    error = machine1.loadRom(1, Settings.current.romPlus21, Settings.defaults.romPlus21, 0x4000);
    if (error != 0) return error;

    error = spec128.commonReset(true);
    if (error != 0) return error;

    periph.clear();
    machinesPeriph.machinesPeriph128();
    periph.update();

    Beta.builtin = false;

    spec48.commonDisplaySetup();

    return 0;
  }

  @Override
  public void memoryMap() {

  }

  @Override
  public int unattachedPort() {
    return 0;
  }

  private class SpecPlus2RamInfo extends RamInfo {
    public SpecPlus2RamInfo(int validPages) {
      this.validPages = validPages;
    }

    boolean portFromUla(int port) {
      return spec48.portFromUla(port);
    }

    public int contendDelay(long time) {
      return spectrum.contendDelay76543210(time);
    }

    public int contendDelayNoMreq(long time) {
      return spectrum.contendDelayNone(time);
    }
  }
}
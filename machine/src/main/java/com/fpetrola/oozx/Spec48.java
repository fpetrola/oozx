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

public class Spec48 extends AbstractSpectrumMachine implements SpectrumMachine {
  private Memory memory;
  private Display display;
  private Machine machine1;
  private MachinesPeriph machinesPeriph;
  protected Spectrum spectrum;
  private Periph periph;

  public Spec48(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Periph periph) {
    super(display);
    this.memory = memory;
    this.display = display;
    this.machine1 = machine;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.periph = periph;
    this.machine = Libspectrum.Machine._48K;
    this.timex = false;
    this.ramInfo = new Spec48RamInfo(this, 3);
  }

  // Check if a port is handled by the ULA
  public boolean portFromUla(int port) {
    // All even ports supplied by ULA
    return (port & 0x0001) == 0;
  }

  // Initialize the Spectrum 48K fuseMachineInfo

  // Reset the Spectrum 48K machine
  public int reset() {
    int error = machine1.loadRom(0, Settings.current.rom48, Settings.defaults.rom48, 0x4000);
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
    display.dirty = display::dirtySinclair;
    display.writeIfDirty = display::writeIfDirtySinclair;
    display.dirtyFlashing = display::dirtyFlashingSinclair;

    memory.displayDirty = memory::displayDirtySinclair;
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
    memory.romcsMap();
  }

  public int unattachedPort() {
    return spectrum.spectrumUnattachedPort();
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3500000, 0, TimingsHandler.FERRANTI_5C_6C);
  }
}
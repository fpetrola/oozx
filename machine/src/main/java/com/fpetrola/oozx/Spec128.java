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

public class Spec128 extends FuseMachineInfo implements SpectrumMachine {
  private Memory memory;
  private Display display;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private Periph periph;

  public Spec128(Memory memory, Display display, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, Periph periph) {
    super(display);
    this.memory = memory;
    this.display = display;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.periph = periph;
    init();
  }

  // Initialize the Spectrum 128K machine
  public SpectrumMachine init() {
    this.machine = Libspectrum.Machine._128K;
    this.id = "128";
    this.timex = false;
    this.ramInfo = new Spec48RamInfo(spec48, 8);

    return this;
  }

  // Reset the Spectrum 128K machine
  @Override
  public int reset() {
    // int error = Machine.loadRom(0, Settings.current.rom128_0, Settings.defaults.rom128_0, 0x4000);
    // if (error != 0) return error;
    // error = Machine.loadRom(1, Settings.current.rom128_1, Settings.defaults.rom128_1, 0x4000);
    // if (error != 0) return error;

    int error = commonReset(true);
    if (error != 0) return error;

    periph.clear();
    machinesPeriph.machinesPeriph128();
    periph.update();

    Beta.builtin = false;

    spec48.commonDisplaySetup();

    return 0;
  }

  // Common reset for Spectrum 128K
  private int commonReset(boolean contention) {
    getRamInfo().locked = false;
    getRamInfo().lastByte = 0;

    getRamInfo().currentPage = 0;
    getRamInfo().currentRom = 0;

    memory.currentScreen = 5;
    memory.screenMask = 0xffff;

    // Odd pages contended on the 128K/+2; loop up to 16 for Scorpion's 256Kb RAM
    for (int i = 0; i < 16; i++) {
      memory.ramSet16kContention(i, (i & 1) != 0 ? contention : false);
    }

    // 0x0000: ROM 0
    memory.map16k(0x0000, memory.mapRom, 0);
    // 0x4000: RAM 5
    memory.map16k(0x4000, memory.mapRam, 5);
    // 0x8000: RAM 2
    memory.map16k(0x8000, memory.mapRam, 2);
    // 0xc000: RAM 0
    memory.map16k(0xc000, memory.mapRam, 0);

    return 0;
  }

  // Write to the 128K memory port (0x7FFD)
  public void memoryPortWrite(int port, byte b) {
    if (getRamInfo().locked) return;

    getRamInfo().lastByte = b;

    memoryMap();

    getRamInfo().locked = (b & 0x20) != 0;
  }

  // Select ROM for 128K
  private void selectRom(int rom) {
    memory.map16k(0x0000, memory.mapRom, rom);
    getRamInfo().currentRom = rom;
  }

  // Select RAM page for 128K
  private void selectPage(int page) {
    memory.map16k(0xc000, memory.mapRam, page);
    getRamInfo().currentPage = page;
  }

  // Map memory for Spectrum 128K
  @Override
  public void memoryMap() {
    byte lastByte = getRamInfo().lastByte;

    int page = lastByte & 0x07;
    int screen = (lastByte & 0x08) != 0 ? 7 : 5;
    int rom = (lastByte & 0x10) >> 4;

    // If screen changed, mark entire display file as dirty
    if (memory.currentScreen != screen) {
      display.updateCritical(0, 0);
      display.refreshMainScreen();
      memory.currentScreen = screen;
    }

    selectRom(rom);
    selectPage(page);

    memory.romcsMap();

  }

  public int unattachedPort() {
    return spectrum.spectrumUnattachedPort();
  }
}

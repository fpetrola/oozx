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

import com.fpetrola.oozx.PeriphDelegate;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.IPeriph;

@Singleton
public class Spec128 extends Spectrum {
  @Inject
  public Spec128(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Sound sound) {
    this(memory, display, machinesPeriph, periph, settings, eventManager, cpu, timer, module, new Spec48RamInfo(8), sound);
  }

  public Spec128(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, RamInfo ramInfo, Sound sound) {
    super(memory, display, eventManager, cpu, timer, module, settings, ramInfo, machinesPeriph, periph, sound);
  }

  @Override
  public int reset() {
    return doReset(settings.current.rom1280, settings.defaults.rom1280, settings.current.rom1281, settings.defaults.rom1281);
  }

  protected int doReset(String rom1280, String rom1281, String rom1282, String rom1283) {
    loadRom(0, rom1280, rom1281, 0x4000);
    loadRom(1, rom1282, rom1283, 0x4000);
    commonReset(true);

    periph.clear();
    machinesPeriph.machinesPeriph128();
    periph.update();

    Beta.builtin = false;

//    spec48.commonDisplaySetup();

    return 0;
  }

  public int commonReset(boolean contention) {
    ramInfo.locked = false;
    ramInfo.lastByte = 0;

    ramInfo.currentPage = 0;
    ramInfo.currentRom = 0;

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
    if (ramInfo.locked) return;

    ramInfo.lastByte = b;

    memoryMap();

    ramInfo.locked = (b & 0x20) != 0;
  }

  // Select ROM for 128K
  private void selectRom(int rom) {
    memory.map16k(0x0000, memory.mapRom, rom);
    ramInfo.currentRom = rom;
  }

  // Select RAM page for 128K
  private void selectPage(int page) {
    memory.map16k(0xc000, memory.mapRam, page);
    ramInfo.currentPage = page;
  }

  // Map memory for Spectrum 128K
  @Override
  public void memoryMap() {
    byte lastByte = ramInfo.lastByte;

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

    memory.romcsMap(ramInfo);

  }

  public int unattachedPort(int port) {
    return spectrumUnattachedPort();
  }

  @Override
  public String getName() {
    return "Spectrum 128K";
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.FERRANTI_7C);
  }

}

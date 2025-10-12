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

public class Spec128 {
  private static Memory memory= Fuse.memory;
  private static Display display;
  private static Machine machine= Fuse.machine;
  private static MachinesPeriph machinesPeriph= Fuse.machinesPeriph;
  private static Spectrum spectrum= Fuse.spectrum;

  // Initialize the Spectrum 128K machine
  public static int init(FuseMachineInfo machine) {
    machine.machine = Libspectrum.Machine._128K;
    machine.id = "128";

    machine.reset = Spec128::reset;
    machine.timex = false;
    machine.ramInfo.portFromUla = Spec48::portFromUla;
    machine.ramInfo.contendDelay = spectrum::contendDelay65432100;
    machine.ramInfo.contendDelayNoMreq = spectrum::contendDelay65432100;
    machine.ramInfo.validPages = 8;
    machine.unattachedPort = spectrum::spectrumUnattachedPort;
    machine.shutdown = null;
    machine.memoryMap = Spec128::memoryMap;

    return 0;
  }

  // Reset the Spectrum 128K machine
  private static int reset() {
    // int error = Machine.loadRom(0, Settings.current.rom128_0, Settings.defaults.rom128_0, 0x4000);
    // if (error != 0) return error;
    // error = Machine.loadRom(1, Settings.current.rom128_1, Settings.defaults.rom128_1, 0x4000);
    // if (error != 0) return error;

    int error = commonReset(true);
    if (error != 0) return error;

    Periph.clear();
    machinesPeriph.machinesPeriph128();
    Periph.update();

    Beta.builtin = false;

    Spec48.commonDisplaySetup();

    return 0;
  }

  // Common reset for Spectrum 128K
  public static int commonReset(boolean contention) {
    FuseMachineInfo machineCurrent = machine.current;

    machineCurrent.ramInfo.locked = false;
    machineCurrent.ramInfo.lastByte = 0;

    machineCurrent.ramInfo.currentPage = 0;
    machineCurrent.ramInfo.currentRom = 0;

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
  public static void memoryPortWrite(int port, byte b) {
    FuseMachineInfo machineCurrent = machine.current;

    if (machineCurrent.ramInfo.locked) return;

    machineCurrent.ramInfo.lastByte = b;

    memoryMap();

    machineCurrent.ramInfo.locked = (b & 0x20) != 0;
  }

  // Select ROM for 128K
  public static void selectRom(int rom) {
    memory.map16k(0x0000, memory.mapRom, rom);
    FuseMachineInfo machineCurrent = machine.current;
    machineCurrent.ramInfo.currentRom = rom;
  }

  // Select RAM page for 128K
  public static void selectPage(int page) {
    memory.map16k(0xc000, memory.mapRam, page);
    FuseMachineInfo machineCurrent = machine.current;
    machineCurrent.ramInfo.currentPage = page;
  }

  // Map memory for Spectrum 128K
  public static int memoryMap() {
    FuseMachineInfo machineCurrent = machine.current;
    byte lastByte = machineCurrent.ramInfo.lastByte;

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

    return 0;
  }
}

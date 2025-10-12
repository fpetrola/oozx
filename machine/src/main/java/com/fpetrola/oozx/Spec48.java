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

public class Spec48 {
  private static Memory memory = Fuse.memory;

  private static Display display = Fuse.display;
  private static Machine machine= Fuse.machine;
  private static MachinesPeriph machinesPeriph= Fuse.machinesPeriph;

  // Check if a port is handled by the ULA
  public static boolean portFromUla(int port) {
    // All even ports supplied by ULA
    return (port & 0x0001) == 0;
  }

  // Initialize the Spectrum 48K machine
  public static int init(FuseMachineInfo machine) {
    machine.machine = Libspectrum.Machine._48K;
    machine.id = "48";

    machine.reset = Spec48::reset;
    machine.timex = false;
    machine.ramInfo.portFromUla = Spec48::portFromUla;
    machine.ramInfo.contendDelay = Spectrum::contendDelay65432100;
    machine.ramInfo.contendDelayNoMreq = Spectrum::contendDelay65432100;
    machine.ramInfo.validPages = 3;
    machine.unattachedPort = Spectrum::spectrumUnattachedPort;
    machine.shutdown = null;
    machine.memoryMap = Spec48::memoryMap;

    return 0;
  }

  // Reset the Spectrum 48K machine
  private static int reset() {
    int error = machine.loadRom(0, Settings.current.rom48, Settings.defaults.rom48, 0x4000);
    if (error != 0) return error;

    Periph.clear();
    machinesPeriph.machinesPeriph48();
    Periph.update();

    Beta.builtin = false;

    memory.currentScreen = 5;
    memory.screenMask = 0xffff;

    commonDisplaySetup();

    return commonReset();
  }

  // Set up common display configuration
  public static void commonDisplaySetup() {
    display.dirty = display::dirtySinclair;
    display.writeIfDirty = display::writeIfDirtySinclair;
    display.dirtyFlashing = display::dirtyFlashingSinclair;

    memory.displayDirty = memory::displayDirtySinclair;
  }

  // Common reset for Spectrum 48K
  public static int commonReset() {
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
  public static int memoryMap() {
    memory.map16k(0x0000, memory.mapRom, 0);
    memory.romcsMap();
    return 0;
  }
}
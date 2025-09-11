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

package com.fpetrola.oozx;// Assuming ported dependencies:
// - Libspectrum (Machine)
// - Machine (FuseMachineInfo, loadRom)
// - Memory (MemoryPage, map16k, ramSet16kContention, mapRomcsMap, displayDirtySinclair, sourceRam)
// - Periph (clear, update)
// - MachinesPeriph (machinesPeriph48)
// - Settings (SettingsInfo, current, default_)
// - Spectrum (RamInfo, contendDelay65432100, unattachedPort, RAM)
// - Display (dirtySinclair, writeIfDirtySinclair, dirtyFlashingSinclair)
// - Beta (builtin)

public class Spec48 {

    // Check if a port is handled by the ULA
    public static boolean portFromUla(int port) {
        // All even ports supplied by ULA
        return (port & 0x0001) == 0;
    }

    // Initialize the Spectrum 48K machine
    public static int init(FuseMachineInfo machine) {
        machine.machine = Libspectrum.Machine._48;
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
        int error = Machine.loadRom(0, Settings.current.rom48, Settings.defaults.rom48, 0x4000);
        if (error != 0) return error;

        Periph.clear();
        MachinesPeriph.machinesPeriph48();
        Periph.update();

        Beta.builtin = false;

        Memory.currentScreen = 5;
        Memory.screenMask = 0xffff;

        commonDisplaySetup();

        return commonReset();
    }

    // Set up common display configuration
    public static void commonDisplaySetup() {
        Display.dirty = Display::dirtySinclair;
        Display.writeIfDirty = Display::writeIfDirtySinclair;
        Display.dirtyFlashing = Display::dirtyFlashingSinclair;

        Memory.displayDirty = Memory::displayDirtySinclair;
    }

    // Common reset for Spectrum 48K
    public static int commonReset() {
        // 0x0000: ROM 0
        Memory.map16k(0x0000, Memory.mapRom, 0);
        // 0x4000: RAM 5, contended
        Memory.ramSet16kContention(5, true);
        Memory.map16k(0x4000, Memory.mapRam, 5);
        // 0x8000: RAM 2, not contended
        Memory.ramSet16kContention(2, false);
        Memory.map16k(0x8000, Memory.mapRam, 2);
        // 0xc000: RAM 0, not contended
        Memory.ramSet16kContention(0, false);
        Memory.map16k(0xc000, Memory.mapRam, 0);

        return 0;
    }

    // Map memory for Spectrum 48K
    public static int memoryMap() {
        Memory.map16k(0x0000, Memory.mapRom, 0);
        Memory.romcsMap();
        return 0;
    }
}

//class Display {
//    static Display.MemoryDisplayDirtyFn dirty;
//    static Runnable writeIfDirty; // Adjust type as needed
//    static Runnable dirtyFlashing; // Adjust type as needed
//
//    static void dirtySinclair(int offset) {
//        // Implementation to be provided
//    }
//
//    static void writeIfDirtySinclair() {
//        // Implementation to be provided
//    }
//
//    static void dirtyFlashingSinclair() {
//        // Implementation to be provided
//    }
//
//    public static int frame() {
//        return 0;
//    }
//}

class MachinesPeriph {
    static void machinesPeriph48() {
        // Implementation to be provided
    }
}

class Beta {
    static boolean builtin;
}
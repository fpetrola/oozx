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

package com.fpetrola.oozx;import java.util.*;

// Assuming ported dependencies:
// - Fuse (for emulator-specific functionality, if needed)
// - StartupManager (StartupManagerModule, register)
// - Pentagon (select1fRead, selectFfRead, pentagon1024MemoryportWrite, pentagon1024V22MemoryportWrite)
// - Periph (Type, Present, Peripheral, Port, register, setPresent)
// - Beta (crWrite, trRead, trWrite, secRead, secWrite, drRead, drWrite, spRead, spWrite)
// - Spec128 (memoryportWrite)
// - SpecPlus3 (memoryport2Write, fdcRead, fdcWrite, fdcStatus)
// - Tc2068 (ayRegisterportRead, ayDataportRead)
// - Ay (registerportWrite, dataportWrite)
// - Machine (FuseMachineInfo, current)

public class MachinesPeriph {
    // Port definitions for 128K memory
    private static final List<Periph.Port> spec128MemoryPorts = List.of(
            new Periph.Port(0x8002, 0x0000, null, (port, b) -> {
                Machine.current.ramInfo.lastByte = b;
                Machine.current.memoryMap.run();
            }),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral spec128Memory = new Periph.Peripheral(
            null, spec128MemoryPorts, false, null
    );

    // Port definitions for +3 memory
    private static final List<Periph.Port> plus3MemoryPorts = List.of(
            new Periph.Port(0xc002, 0x4000, null, Spec128::memoryPortWrite),
            new Periph.Port(0xf002, 0x1000, null, SpecPlus3::memoryPort2Write),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral plus3Memory = new Periph.Peripheral(
            null, plus3MemoryPorts, false, null
    );

    // Port definitions for uPD765 FDC
    private static final List<Periph.Port> upd765Ports = List.of(
            new Periph.Port(0xf002, 0x3000, SpecPlus3::fdcRead, SpecPlus3::fdcWrite),
            new Periph.Port(0xf002, 0x2000, SpecPlus3::fdcStatus, null),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral upd765 = new Periph.Peripheral(
            null, upd765Ports, false, null
    );

    // Port definitions for Spectrum SE memory
    private static final List<Periph.Port> seMemoryPorts = List.of(
            new Periph.Port(0xffff, 0x7ffd, null, (port, b) -> {
                Machine.current.ramInfo.lastByte = b;
                Machine.current.memoryMap.run();
            }),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral seMemory = new Periph.Peripheral(
            null, seMemoryPorts, false, null
    );

    // Port definitions for TC2068 AY chip with joystick
    private static final List<Periph.Port> tc2068AyPorts = List.of(
            new Periph.Port(0x00ff, 0x00f5, Tc2068::ayRegisterportRead, Ay::registerportWrite),
            new Periph.Port(0x00ff, 0x00f6, Tc2068::ayDataportRead, Ay::dataportWrite),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral tc2068Ay = new Periph.Peripheral(
            null, tc2068AyPorts, false, null
    );

    // Port definitions for Beta128 Pentagon
    private static final List<Periph.Port> beta128PentagonPorts = List.of(
            new Periph.Port(0x00ff, 0x001f, Pentagon::select1fRead, Beta::crWrite),
            new Periph.Port(0x00ff, 0x003f, Beta::trRead, Beta::trWrite),
            new Periph.Port(0x00ff, 0x005f, Beta::secRead, Beta::secWrite),
            new Periph.Port(0x00ff, 0x007f, Beta::drRead, Beta::drWrite),
            new Periph.Port(0x00ff, 0x00ff, Pentagon::selectFfRead, Beta::spWrite),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral beta128Pentagon = new Periph.Peripheral(
            null, beta128PentagonPorts, false, null
    );

    // Port definitions for Beta128 Pentagon (late)
    private static final List<Periph.Port> beta128PentagonLatePorts = List.of(
            new Periph.Port(0x00ff, 0x001f, Pentagon::select1fRead, Beta::crWrite),
            new Periph.Port(0x00ff, 0x003f, Beta::trRead, Beta::trWrite),
            new Periph.Port(0x00ff, 0x005f, Beta::secRead, Beta::secWrite),
            new Periph.Port(0x00ff, 0x007f, Beta::drRead, Beta::drWrite),
            new Periph.Port(0x00ff, 0x00ff, Beta::spRead, Beta::spWrite),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral beta128PentagonLate = new Periph.Peripheral(
            null, beta128PentagonLatePorts, false, null
    );

    // Port definitions for Pentagon 1024 memory
    private static final List<Periph.Port> pentagon1024MemoryPorts = List.of(
            new Periph.Port(0xc002, 0x4000, null, Pentagon::pentagon1024MemoryportWrite),
            new Periph.Port(0xf008, 0xe000, null, Pentagon::pentagon1024V22MemoryportWrite),
            new Periph.Port(0, 0, null, null)
    );

    private static final Periph.Peripheral pentagon1024Memory = new Periph.Peripheral(
            null, pentagon1024MemoryPorts, false, null
    );

    // Initialize machine-specific peripherals
    private static int init(Object context) {
        Periph.register(Periph.Type._128_MEMORY, spec128Memory);
        Periph.register(Periph.Type.PLUS3_MEMORY, plus3Memory);
        Periph.register(Periph.Type.UPD765, upd765);
        Periph.register(Periph.Type.SE_MEMORY, seMemory);
        Periph.register(Periph.Type.AY_TIMEX_WITH_JOYSTICK, tc2068Ay);
        Periph.register(Periph.Type.BETA128_PENTAGON, beta128Pentagon);
        Periph.register(Periph.Type.BETA128_PENTAGON_LATE, beta128PentagonLate);
        Periph.register(Periph.Type.PENTAGON1024_MEMORY, pentagon1024Memory);
        return 0;
    }

    // Register startup for machine-specific peripherals
    public static void registerStartup() {
        StartupManagerModule[] dependencies = {StartupManagerModule.SETUID};
        StartupManager.register(StartupManagerModule.MACHINES_PERIPH, dependencies, MachinesPeriph::init, null, null);
    }

    // Base peripherals available on all machines
    private static void basePeripherals() {
        Periph.setPresent(Periph.Type.DIVIDE, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.DIVMMC, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.KEMPSTON, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.KEMPSTON_MOUSE, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.SIMPLEIDE, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.SPECCYBOOT, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.SPECTRANET, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.ULA, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.ZXATASP, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.ZXCF, Periph.Present.OPTIONAL);
    }

    // Base peripherals for 48K and 128K machines
    private static void basePeripherals48128() {
        basePeripherals();
        Periph.setPresent(Periph.Type.BETA128, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.INTERFACE1, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.INTERFACE2, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.MULTIFACE_128, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.OPUS, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.PLUSD, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.SPECDRUM, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.USOURCE, Periph.Present.OPTIONAL);
    }

    // Peripherals for 48K and similar machines
    public static void machinesPeriph48() {
        basePeripherals48128();
        Periph.setPresent(Periph.Type.FULLER, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.MELODIK, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.MULTIFACE_1, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.TTX2000S, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.ZXPRINTER, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.DIDAKTIK80, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.DISCIPLE, Periph.Present.OPTIONAL);
    }

    // Peripherals for 128K and similar machines
    public static void machinesPeriph128() {
        basePeripherals48128();
        Periph.setPresent(Periph.Type.AY, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type._128_MEMORY, Periph.Present.ALWAYS);
    }

    // Peripherals for +3 and similar machines
    public static void machinesPeriphPlus3() {
        basePeripherals();
        Periph.setPresent(Periph.Type.AY_PLUS3, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.MULTIFACE_3, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.PARALLEL_PRINTER, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.PLUS3_MEMORY, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.ZXMMC, Periph.Present.OPTIONAL);
    }

    // Peripherals for TC2068 and TS2068
    public static void machinesPeriphTimex() {
        basePeripherals();
        Periph.setPresent(Periph.Type.ULA, Periph.Present.NEVER);
        Periph.setPresent(Periph.Type.ULA_FULL_DECODE, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.SCLD, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.AY_TIMEX_WITH_JOYSTICK, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.INTERFACE2, Periph.Present.OPTIONAL);
        Periph.setPresent(Periph.Type.ZXPRINTER_FULL_DECODE, Periph.Present.OPTIONAL);
    }

    // Peripherals for Pentagon and Scorpion
    public static void machinesPeriphPentagon() {
        basePeripherals();
        Periph.setPresent(Periph.Type._128_MEMORY, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.AY, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.ULA, Periph.Present.NEVER);
        Periph.setPresent(Periph.Type.ULA_FULL_DECODE, Periph.Present.ALWAYS);
        Periph.setPresent(Periph.Type.KEMPSTON, Periph.Present.NEVER);
    }
}

// Supporting classes (minimal definitions for context)
class Ay {
    static void registerportWrite(int port, byte data) {
        // Implementation to be provided
    }

    static void dataportWrite(int port, byte data) {
        // Implementation to be provided
    }
}
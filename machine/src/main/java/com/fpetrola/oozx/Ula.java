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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

// Assuming ported dependencies:
// - Libspectrum (with Snap, MachineCapability)
// - Machine (FuseMachineInfo, current, ram)
// - Display (setLoresBorder)
// - Sound (beeper)
// - Keyboard (read)
// - Tape (microphone)
// - Loader (detectLoader)
// - Settings (SettingsInfo, current)
// - Debugger (systemVariableRegister)
// - Module (ModuleInfo, register)
// - Periph (Periph, PeriphPort, PeriphType, register)
// - PhantomTypist (ulaRead)
// - Spec128 (memoryportWrite)
// - SpecPlus3 (memoryport2WriteInternal)
// - StartupManager (StartupManagerModule, register)
// - Spectrum (tstates, memoryMapRead, MEMORY_PAGE_SIZE_LOGARITHM)

public class Ula {

    public static final int CONTENTION_SIZE = 80000;

    // How much contention do we get at every tstate when MREQ is active?
    public static byte[] contention = new byte[CONTENTION_SIZE];

    // And how much when it is inactive
    public static byte[] contentionNoMreq = new byte[CONTENTION_SIZE];

    private static byte lastByte;

    // What to return if no other input pressed; depends on the last byte output to the ULA
    private static byte defaultValue;

    // Debugger system variables
    private static final String DEBUGGER_TYPE_STRING = "ula";
    private static final String LAST_BYTE_DETAIL_STRING = "last";
    private static final String TSTATES_DETAIL_STRING = "tstates";
    private static final String MEM7FFD_DETAIL_STRING = "mem7ffd";
    private static final String MEM1FFD_DETAIL_STRING = "mem1ffd";

    // Functional interfaces for debugger system variables
    @FunctionalInterface
    interface DebuggerGetter {
        long apply();
    }

    @FunctionalInterface
    interface DebuggerSetter {
        void apply(long value);
    }

    // Module info for ULA
    private static final ModuleInfo ulaModuleInfo = new ModuleInfo(
            null, // reset
            null, // romcs
            null, // snapshotEnabled
            Ula::fromSnapshot, // snapshotFrom
            Ula::toSnapshot // snapshotTo
    );

    // Peripheral ports for ULA
    private static final PeriphPort[] ulaPorts = {
            new PeriphPort(0x0001, 0x0000, Ula::read, Ula::write),
            new PeriphPort(0, 0, null, null)
    };

    private static final Periph ulaPeriph = new Periph(
            null, // option
            ulaPorts,
            false, // hardReset
            null // activate
    );

    // Peripheral ports for full decode
    private static final PeriphPort[] ulaPortsFullDecode = {
            new PeriphPort(0x00ff, 0x00fe, Ula::read, Ula::write),
            new PeriphPort(0, 0, null, null)
    };

    private static final Periph ulaPeriphFullDecode = new Periph(
            null, // option
            ulaPortsFullDecode,
            false, // hardReset
            null // activate
    );

    // Adapter for debugger to get last byte
    private static long getLastByte() {
        return lastByte & 0xFF;
    }

    // Adapter for debugger to get tstates
    private static long getTstates() {
        return Spectrum.tstates;
    }

    // Adapter for debugger to set tstates
    private static void setTstates(long value) {
        Spectrum.tstates = value;
    }

    // Adapter for debugger to get 7ffd
    private static long get7ffd() {
        return Machine.current.ram.lastByte & 0xFF;
    }

    // Adapter for debugger to set 7ffd
    private static void set7ffd(long value) {
        Spec128.memoryportWrite(0, (byte) value);
    }

    // Adapter for debugger to get 1ffd
    private static long get1ffd() {
        return Machine.current.ram.lastByte2 & 0xFF;
    }

    // Adapter for debugger to set 1ffd
    private static void set1ffd(long value) {
        SpecPlus3.memoryport2WriteInternal(0, (byte) value);
    }

    // Initialize ULA module
    private static int init(Object context) {
        Module.register(ulaModuleInfo);

        Periph.register(PeriphType.ULA, ulaPeriph);
        Periph.register(PeriphType.ULA_FULL_DECODE, ulaPeriphFullDecode);

        Debugger.systemVariableRegister(
                DEBUGGER_TYPE_STRING, LAST_BYTE_DETAIL_STRING, Ula::getLastByte, null);
        Debugger.systemVariableRegister(
                DEBUGGER_TYPE_STRING, TSTATES_DETAIL_STRING, Ula::getTstates, Ula::setTstates);
        Debugger.systemVariableRegister(
                DEBUGGER_TYPE_STRING, MEM7FFD_DETAIL_STRING, Ula::get7ffd, Ula::set7ffd);
        Debugger.systemVariableRegister(
                DEBUGGER_TYPE_STRING, MEM1FFD_DETAIL_STRING, Ula::get1ffd, Ula::set1ffd);

        defaultValue = (byte) 0xff;

        return 0;
    }

    // Register ULA with startup manager
    public static void registerStartup() {
        StartupManagerModule[] dependencies = {
                StartupManagerModule.DEBUGGER,
                StartupManagerModule.SETUID
        };
        StartupManager.register(StartupManagerModule.ULA, dependencies, Ula::init, null, null);
    }

    // Read from ULA port
    private static byte read(int port, byte[] attached) {
        byte r = defaultValue;
        attached[0] = (byte) 0xff;

        Loader.detectLoader();

        r &= PhantomTypist.ulaRead(port);
        r &= Keyboard.read(port >> 8);
        if (Tape.microphone) r ^= 0x40;

        return r;
    }

    // Write to ULA port
    private static void write(int port, byte b) {
        lastByte = b;

        Display.setLoresBorder(b & 0x07);
        Sound.beeper(Spectrum.tstates,
                ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || Tape.microphone ? 1 : 0));

        if (Machine.current.timex) {
            defaultValue = (byte) 0x5f;
        } else if ((Machine.current.capabilities & Libspectrum.MachineCapability.PLUS3_MEMORY) != 0) {
            defaultValue = (byte) 0xbf;
        } else if ((Machine.current.capabilities & Libspectrum.MachineCapability._128_MEMORY) != 0 || !Settings.current.issue2) {
            defaultValue = (byte) ((b & 0x10) != 0 ? 0xff : 0xbf);
        } else {
            defaultValue = (byte) ((b & 0x18) != 0 ? 0xff : 0xbf);
        }
    }

    // Get the last byte written to the ULA
    public static byte lastByte() {
        return lastByte;
    }

    // Get the tape level from the last byte
    public static byte tapeLevel() {
        return (byte) (lastByte & 0x08);
    }

    // Load ULA state from snapshot
    private static void fromSnapshot(Libspectrum.Snap snap) {
        write(0x00fe, Libspectrum.snapOutUla(snap));
        Spectrum.tstates = Libspectrum.snapTstates(snap);
        Settings.current.issue2 = Libspectrum.snapIssue2(snap);
    }

    // Save ULA state to snapshot
    private static void toSnapshot(Libspectrum.Snap snap) {
        Libspectrum.snapSetOutUla(snap, lastByte);
        Libspectrum.snapSetTstates(snap, Spectrum.tstates);
        Libspectrum.snapSetIssue2(snap, Settings.current.issue2);
    }

    // Handle contention for port access (early phase)
    public static void contendPortEarly(int port) {
        if (Spectrum.memoryMapRead[port >> Memory.PAGE_SIZE_LOGARITHM].contended) {
            Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
        }
        Spectrum.tstates++;
    }

    // Handle contention for port access (late phase)
    public static void contendPortLate(int port) {
        if (Machine.current.ram.portFromUla.apply(port)) {
            Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
            Spectrum.tstates += 2;
        } else {
            if (Spectrum.memoryMapRead[port >> Memory.PAGE_SIZE_LOGARITHM].contended) {
                Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
                Spectrum.tstates++;
                Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
                Spectrum.tstates++;
                Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
            } else {
                Spectrum.tstates += 2;
            }
        }
    }
}

// Assuming supporting classes
class ModuleInfo {
    ModuleResetFn reset;
    ModuleRomcsFn romcs;
    ModuleSnapshotEnabledFn snapshotEnabled;
    Consumer<Libspectrum.Snap> snapshotFrom;
    Consumer<Libspectrum.Snap> snapshotTo;

    ModuleInfo(ModuleResetFn reset, ModuleRomcsFn romcs, ModuleSnapshotEnabledFn snapshotEnabled,
               Consumer<Libspectrum.Snap> snapshotFrom, Consumer<Libspectrum.Snap> snapshotTo) {
        this.reset = reset;
        this.romcs = romcs;
        this.snapshotEnabled = snapshotEnabled;
        this.snapshotFrom = snapshotFrom;
        this.snapshotTo = snapshotTo;
    }
}

class PeriphPort {
    int mask;
    int value;
    BiFunction<Integer, byte[], Byte> read; // Returns byte, takes port and attached array
    BiConsumer<Integer, Byte> write; // Takes port and byte

    PeriphPort(int mask, int value, BiFunction<Integer, byte[], Byte> read, BiConsumer<Integer, Byte> write) {
        this.mask = mask;
        this.value = value;
        this.read = read;
        this.write = write;
    }
}

class Periph {
    Object option;
    PeriphPort[] ports;
    boolean hardReset;
    Runnable activate;

    Periph(Object option, PeriphPort[] ports, boolean hardReset, Runnable activate) {
        this.option = option;
        this.ports = ports;
        this.hardReset = hardReset;
        this.activate = activate;
    }

    public static void clear() {

    }

    public static void update() {

    }

    public static void register(PeriphType periphType, Periph ulaPeriph) {

    }

    public static void end() {

    }
}

enum PeriphType {
    ULA,
    ULA_FULL_DECODE
}
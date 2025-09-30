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
// - Libspectrum (Machine, MachineCapability, Error, Rzx)
// - Debugger (DebuggerMode, BreakpointType, eventRegister)
// - Event (add, EventType)
// - Fuse (abort)
// - If1 (updateMenu)
// - Multiface (statusUpdate)
// - Ula (contendPortEarly, contendPortLate)
// - Rzx (playback, recording, storeByte, stopPlayback)
// - Settings (SettingsInfo, current)
// - Ui (MenuItem, menuActivate, mousePresent, mouseGrabbed, mouseGrab, mouseRelease)
// - Machine (FuseMachineInfo, current, reset)
// - SpecPlus3 (updateFdd)
// - Spectrum (tstates, unattachedPort)

public class Periph {
    // Enum for peripheral types
    public enum Type {
        UNKNOWN,
        _128_MEMORY,
        AY,
        AY_FULL_DECODE,
        AY_PLUS3,
        AY_TIMEX,
        AY_TIMEX_WITH_JOYSTICK,
        BETA128,
        BETA128_PENTAGON,
        BETA128_PENTAGON_LATE,
        COVOX_DD,
        COVOX_FB,
        DIVIDE,
        DIVMMC,
        PLUSD,
        DIDAKTIK80,
        DISCIPLE,
        FULLER,
        INTERFACE1,
        INTERFACE2,
        KEMPSTON,
        KEMPSTON_LOOSE,
        KEMPSTON_MOUSE,
        MELODIK,
        MULTIFACE_1,
        MULTIFACE_128,
        MULTIFACE_3,
        OPUS,
        PARALLEL_PRINTER,
        PENTAGON1024_MEMORY,
        PLUS3_MEMORY,
        SCLD,
        SE_MEMORY,
        SIMPLEIDE,
        SPECCYBOOT,
        SPECDRUM,
        SPECTRANET,
        TTX2000S,
        ULA,
        ULA_FULL_DECODE,
        UPD765,
        USOURCE,
        ZXATASP,
        ZXCF,
        ZXMMC,
        ZXPRINTER,
        ZXPRINTER_FULL_DECODE
    }

    // Enum for peripheral presence
    public enum Present {
        NEVER,
        OPTIONAL,
        ALWAYS
    }

    // Functional interfaces for port read/write
    @FunctionalInterface
    public interface PortReadFunction {
        byte apply(int port, byte[] attached);
    }

    @FunctionalInterface
    public interface PortWriteFunction {
        void apply(int port, byte data);
    }

    @FunctionalInterface
    interface ActivateFunction {
        void apply();
    }

    // Structure for port response
    public static class Port {
        int mask;
        int value;
        PortReadFunction read;
        PortWriteFunction write;

        public Port(int mask, int value, PortReadFunction read, PortWriteFunction write) {
            this.mask = mask;
            this.value = value;
            this.read = read;
            this.write = write;
        }
    }

    // Structure for peripheral information
    public static class Peripheral {
        boolean[] option; // Preferences option controlling this peripheral
        List<Port> ports; // List of ports this peripheral responds to
        boolean hardReset; // Hard reset required when added/removed
        ActivateFunction activate; // Function called when peripheral is activated

        public Peripheral(boolean[] option, List<Port> ports, boolean hardReset, ActivateFunction activate) {
            this.option = option;
            this.ports = ports;
            this.hardReset = hardReset;
            this.activate = activate;
        }
    }

    // Private structure for peripheral data
    private static class PrivatePeripheral {
        Present present;
        boolean active;
        Peripheral peripheral;

        PrivatePeripheral(Present present, boolean active, Peripheral peripheral) {
            this.present = present;
            this.active = active;
            this.peripheral = peripheral;
        }
    }

    // Private structure for port response with peripheral type
    private static class PrivatePort {
        Type type;
        Port port;

        PrivatePort(Type type, Port port) {
            this.type = type;
            this.port = port;
        }
    }

    // All peripherals known to the system
    private static Map<Type, PrivatePeripheral> peripherals = null;

    // List of currently active ports
    private static List<PrivatePort> ports = new ArrayList<>();

    // Strings for debugger events
    private static final String PAGE_EVENT_STRING = "page";
    private static final String UNPAGE_EVENT_STRING = "unpage";

    // Register a peripheral with the system
    public static void register(Type type, Peripheral peripheral) {
        if (peripherals == null) {
            peripherals = new HashMap<>();
        }

        PrivatePeripheral privatePeriph = new PrivatePeripheral(Present.NEVER, false, peripheral);
        peripherals.put(type, privatePeriph);
    }

    // Set whether a peripheral can be present on this machine
    public static void setPresent(Type type, Present present) {
        PrivatePeripheral typeData = peripherals.get(type);
        if (typeData != null) {
            typeData.present = present;
        }
    }

    // Mark a specific peripheral as (in)active
    public static boolean activateType(Type type, boolean active) {
        PrivatePeripheral privatePeriph = peripherals.get(type);
        if (privatePeriph == null || privatePeriph.active == active) {
            return false;
        }

        privatePeriph.active = active;

        if (active) {
            if (privatePeriph.peripheral.activate != null) {
                privatePeriph.peripheral.activate.apply();
            }
            for (Port port : privatePeriph.peripheral.ports) {
                if (port.mask == 0) break; // End of port list
                ports.add(new PrivatePort(type, port));
            }
        } else {
            ports.removeIf(p -> p.type == type);
        }

        return true;
    }

    // Check if a specific peripheral is active
    public static boolean isActive(Type type) {
        PrivatePeripheral typeData = peripherals.get(type);
        return typeData != null && typeData.active;
    }

    // Empty out the list of peripherals
    public static void clear() {
        ports.clear();
        if (peripherals != null) {
            peripherals.forEach((type, data) -> {
                data.present = Present.NEVER;
                data.active = false;
            });
        }
    }

    // Clean up peripherals at the end of emulation
    public static void end() {
        ports.clear();
        if (peripherals != null) {
            peripherals.clear();
            peripherals = null;
        }
    }

    // Structure for peripheral data during read/write
    private static class PeripheralData {
        int port;
        byte attached;
        byte value;

        PeripheralData(int port, byte attached, byte value) {
            this.port = port;
            this.attached = attached;
            this.value = value;
        }
    }

    // Read a byte from a port, taking the appropriate time
    public static byte readPort(int port) {
        Ula.contendPortEarly(port);
        Ula.contendPortLate(port);
        byte b = readPortInternal(port);

        // Special case for 128K/+2 machines
        if ((port & 0x8002) == 0 &&
                (Machine.current.machine == Libspectrum.Machine._128K ||
                        Machine.current.machine == Libspectrum.Machine.PLUS2)) {
            writePortInternal(0x7ffd, b);
        }

        Spectrum.tstates++;

        return -1;
    }

    // Read a byte from a port, taking no time
    public static byte readPortInternal(int port) {
        if (Debugger.mode != DebuggerMode.INACTIVE) {
            Debugger.check(DebuggerBreakpointType.PORT_READ, port);
        }

        // Handle RZX playback
        if (Rzx.playback) {
            try {
                byte value = Rzx.playback();
                return value;
            } catch (Libspectrum.Error error) {
                Rzx.stopPlayback(true);
                //                EventManager.eventAdd(Spectrum.tstates, Event.Type.NULL);
                EventManager.eventAdd(Spectrum.tstates, -1);
                return readPortInternal(port); // Retry
            }
        }

        // Normal port read
        PeripheralData callbackInfo = new PeripheralData(port, (byte) 0x00, (byte) 0xff);
        for (PrivatePort privatePort : ports) {
            Port portData = privatePort.port;
            if (portData.read != null && (callbackInfo.port & portData.mask) == portData.value) {
                byte[] attached = new byte[]{0};
                byte value = portData.read.apply(callbackInfo.port, attached);
                callbackInfo.value &= (byte) (value | callbackInfo.attached);
                callbackInfo.attached |= attached[0]!= 0 ? (byte) 0xff : 0;
            }
        }

        if (callbackInfo.attached != (byte) 0xff) {
            callbackInfo.value = mergeFloatingBus(callbackInfo.value, callbackInfo.attached,
                (byte) Machine.current.unattachedPort.apply());
        }

        if (Rzx.recording) {
            Rzx.storeByte(callbackInfo.value);
        }

        return callbackInfo.value;
    }

    // Merge the read value with the floating bus
    public static byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
        return (byte) (value & (floatingBus | attached));
    }

    // Write a byte to a port, taking the appropriate time
    public static void writePort(int port, byte b) {
        Ula.contendPortEarly(port);
        writePortInternal(port, b);
        Ula.contendPortLate(port);
        Spectrum.tstates++;
    }

    // Write a byte to a port, taking no time
    public static void writePortInternal(int port, byte b) {
        if (Debugger.mode != DebuggerMode.INACTIVE) {
            Debugger.check(DebuggerBreakpointType.PORT_WRITE, port);
        }

        PeripheralData callbackInfo = new PeripheralData(port, (byte) 0, b);
        for (PrivatePort privatePort : ports) {
            Port portData = privatePort.port;
            if (portData.write != null && (callbackInfo.port & portData.mask) == portData.value) {
                portData.write.apply(callbackInfo.port, callbackInfo.value);
            }
        }
    }

    // Update cartridge menu
    private static void updateCartridgeMenu() {
        boolean dock = (Machine.current.capabilities & Libspectrum.MachineCapability.TIMEX_DOCK) != 0;
        boolean if2 = isActive(Type.INTERFACE2);
        boolean cartridge = dock || if2;

        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE, cartridge);
        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_DOCK, dock);
        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_IF2, if2);
    }

    // Update IDE menu
    private static void updateIdeMenu() {
        boolean simpleide = Settings.current.simpleideActive;
        boolean zxatasp = Settings.current.zxataspActive;
        boolean zxcf = Settings.current.zxcfActive;
        boolean divide = Settings.current.divideEnabled;
        boolean divmmc = Settings.current.divmmcEnabled;
        boolean zxmmc = Settings.current.zxmmcEnabled;
        boolean ide = simpleide || zxatasp || zxcf || divide || divmmc || zxmmc;

        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE, ide);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_SIMPLE8BIT, simpleide);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXATASP, zxatasp);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXCF, zxcf);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_DIVIDE, divide);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_DIVMMC, divmmc);
        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXMMC, zxmmc);
    }

    // Update peripherals status
    private static void updatePeripheralsStatus() {
        Ui.menuActivate(Ui.MenuItem.MEDIA_IF1, isActive(Type.INTERFACE1));
        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_IF2, isActive(Type.INTERFACE2));
        updateCartridgeMenu();
        updateIdeMenu();
//        If1.updateMenu();
//        Multiface.statusUpdate();
//        SpecPlus3.updateFdd();
    }

    // Disable optional peripherals
    public static void disableOptional() {
        if (Ui.mousePresent && Ui.mouseGrabbed) {
            Ui.mouseGrabbed = Ui.mouseRelease(true);
        }

        peripherals.forEach((type, privatePeriph) -> {
            if (privatePeriph.present == Present.NEVER || privatePeriph.present == Present.OPTIONAL) {
                if (privatePeriph.peripheral.option != null) {
                    privatePeriph.peripheral.option[0] = false;
                }
            }
        });

        updatePeripheralsStatus();
    }

    // Update peripherals and determine if a hard reset is needed
    public static boolean update() {
        boolean[] needsHardReset = {false};

        if (Ui.mousePresent) {
            if (Settings.current.kempstonMouse) {
                if (!Ui.mouseGrabbed) {
                    Ui.mouseGrabbed = Ui.mouseGrab(true);
                }
            } else {
                if (Ui.mouseGrabbed) {
                    Ui.mouseGrabbed = Ui.mouseRelease(true);
                }
            }
        }

        peripherals.forEach((type, privatePeriph) -> {
            boolean active = switch (privatePeriph.present) {
                case NEVER -> false;
                case OPTIONAL -> privatePeriph.peripheral.option != null && privatePeriph.peripheral.option[0];
                case ALWAYS -> true;
            };
            boolean changed = activateType(type, active);
            needsHardReset[0] |= changed && privatePeriph.peripheral.hardReset;
        });

        updatePeripheralsStatus();
        Machine.current.memoryMap.run();

        return needsHardReset[0];
    }

    // Perform post-update hook
    public static void postHook() {
        if (update()) {
            Machine.reset(true);
        }
    }

    // Check if a hard reset is needed without activating/deactivating
    public static boolean postCheck() {
        boolean[] needsHardReset = {false};

        peripherals.forEach((type, privatePeriph) -> {
            boolean active = switch (privatePeriph.present) {
                case NEVER -> false;
                case OPTIONAL -> privatePeriph.peripheral.option != null && privatePeriph.peripheral.option[0];
                case ALWAYS -> true;
            };
            boolean needsReset = privatePeriph != null && privatePeriph.active != active && privatePeriph.peripheral.hardReset;
            needsHardReset[0] |= needsReset;
        });

        return needsHardReset[0];
    }

    // Register debugger page/unpage events for a peripheral
    public static void registerPagingEvents(String typeString, int[] pageEvent, int[] unpageEvent) {
        pageEvent[0] = Debugger.eventRegister(typeString, PAGE_EVENT_STRING);
        unpageEvent[0] = Debugger.eventRegister(typeString, UNPAGE_EVENT_STRING);
    }
}
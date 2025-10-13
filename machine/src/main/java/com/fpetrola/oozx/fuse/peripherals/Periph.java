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

package com.fpetrola.oozx.fuse.peripherals;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.KempstonLoosePeriphPeripheral;
import com.fpetrola.oozx.fuse.KempstonStrictPeripheral;
import com.fpetrola.oozx.fuse.machine.Spec128;
import com.fpetrola.oozx.fuse.machine.SpecPlus2;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.EventManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Periph {
  private EventManager eventManager;
  private Ula ula;
  private Supplier<SpectrumMachine> machine;
  private TStatesHolder tStatesHolder;

  public Periph(EventManager eventManager, Ula ula, Supplier<SpectrumMachine> machine, TStatesHolder tStatesHolder) {
    this.eventManager = eventManager;
    this.ula = ula;
    this.machine = machine;
    this.tStatesHolder = tStatesHolder;
  }

  // Enum for peripheral types
  public enum Type {
    UNKNOWN,
    _128_MEMORY(Spec128MemoryPeripheral.class),
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
    KEMPSTON(KempstonStrictPeripheral.class),
    KEMPSTON_LOOSE(KempstonLoosePeriphPeripheral.class),
    KEMPSTON_MOUSE,
    MELODIK,
    MULTIFACE_1,
    MULTIFACE_128,
    MULTIFACE_3,
    OPUS,
    PARALLEL_PRINTER,
    PENTAGON1024_MEMORY,
    PLUS3_MEMORY(SpecPlus3MemoryPeripheral.class),
    SCLD,
    SE_MEMORY,
    SIMPLEIDE,
    SPECCYBOOT,
    SPECDRUM,
    SPECTRANET,
    TTX2000S,
    ULA(UlaPeripheral.class),
    ULA_FULL_DECODE(UlaFullDecodePeripheral.class),
    UPD765,
    USOURCE,
    ZXATASP,
    ZXCF,
    ZXMMC,
    ZXPRINTER,
    ZXPRINTER_FULL_DECODE;

    private final Class<? extends ZxPeripheral> aClass;

    Type(Class<? extends ZxPeripheral> aClass) {
      this.aClass = aClass;
    }

    Type() {
      this(GenericZxPeripheral.class);
    }

    public Class<? extends ZxPeripheral> getZxPeripheralClass() {
      return aClass;
    }
  }

  // Enum for peripheral presence
  public enum Present {
    NEVER,
    OPTIONAL,
    ALWAYS
  }

  // Private structure for peripheral data
  private class PrivatePeripheral {
    Present present;
    boolean active;
    ZxPeripheral peripheral;

    PrivatePeripheral(Present present, boolean active, ZxPeripheral peripheral) {
      this.present = present;
      this.active = active;
      this.peripheral = peripheral;
    }
  }

  // Private structure for port response with peripheral type
  private class PrivatePort {
    Class<? extends ZxPeripheral> type;
    PortHandler port;

    PrivatePort(Class<? extends ZxPeripheral> type, PortHandler port) {
      this.type = type;
      this.port = port;
    }
  }

  // All peripherals known to the system
  private Map<Class<? extends ZxPeripheral>, PrivatePeripheral> peripherals = null;

  // List of currently active ports
  private List<PrivatePort> ports = new ArrayList<>();

  // Strings for debugger events
  private final String PAGE_EVENT_STRING = "page";
  private final String UNPAGE_EVENT_STRING = "unpage";

//  // Register a peripheral with the system
//  public  void register(Type type, Peripheral peripheral) {
//    ZxPeripheralAdapter peripheral1 = new ZxPeripheralAdapter(type, peripheral);
//
//    register(peripheral1);
//  }

  public void register(ZxPeripheral zxPeripheral) {
    if (peripherals == null) {
      peripherals = new HashMap<>();
    }

    PrivatePeripheral privatePeriph = new PrivatePeripheral(Present.NEVER, false, zxPeripheral);
    peripherals.put(zxPeripheral.getClass(), privatePeriph);
  }

  // Set whether a peripheral can be present on this machine
  public void setPresent(Type type, Present present) {
    Class<? extends ZxPeripheral> zxPeripheralClass = type.getZxPeripheralClass();
    setPresent(zxPeripheralClass, present);
  }

  public void setPresent(Class<? extends ZxPeripheral> zxPeripheralClass, Present present) {
    PrivatePeripheral typeData = peripherals.get(zxPeripheralClass);
    if (typeData != null) {
      typeData.present = present;
    }
  }

  // Mark a specific peripheral as (in)active
  public boolean activateType(Class<? extends ZxPeripheral> type, boolean active) {
    PrivatePeripheral privatePeriph = peripherals.get(type);
    if (privatePeriph == null || privatePeriph.active == active) {
      return false;
    }

    privatePeriph.active = active;

    if (active) {
      if (privatePeriph.peripheral.canActivate()) {
        privatePeriph.peripheral.activate();
      }
      for (PortHandler port : privatePeriph.peripheral.getPorts()) {
        ports.add(new PrivatePort(type, port));
      }
    } else {
      ports.removeIf(p -> p.type == type);
    }

    return true;
  }

  // Check if a specific peripheral is active
  public boolean isActive(Type type) {
    PrivatePeripheral typeData = peripherals.get(type);
    return typeData != null && typeData.active;
  }

  // Empty out the list of peripherals
  public void clear() {
    ports.clear();
    if (peripherals != null) {
      peripherals.forEach((type, data) -> {
        data.present = Present.NEVER;
        data.active = false;
      });
    }
  }

  // Clean up peripherals at the end of emulation
  public void end() {
    ports.clear();
    if (peripherals != null) {
      peripherals.clear();
      peripherals = null;
    }
  }

  // Structure for peripheral data during read/write
  private class PeripheralData {
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
  public byte readPort(int port) {
    ula.contendPortEarly(port);
    ula.contendPortLate(port);
    byte b = readPortInternal(port);

    // Special case for 128K/+2 machines
    if ((port & 0x8002) == 0 &&
        (machine.get().getClass() == Spec128.class ||
            machine.get().getClass() == SpecPlus2.class)) {
      writePortInternal(0x7ffd, b);
    }

    tStatesHolder.setTstates(tStatesHolder.getTstates() + 1);
//        b= -1;
    return b;
  }

  // Read a byte from a port, taking no time
  public byte readPortInternal(int port) {
    // Handle RZX playback
    if (Rzx.playback) {
      try {
        byte value = Rzx.playback();
        return value;
      } catch (Libspectrum.Error error) {
        Rzx.stopPlayback(true);
        //                EventManager.eventAdd(tStatesHolder.tstates, Event.Type.NULL);
        eventManager.eventAdd(tStatesHolder.getTstates(), -1);
        return readPortInternal(port); // Retry
      }
    }

    // Normal port read
    PeripheralData callbackInfo = new PeripheralData(port, (byte) 0x00, (byte) 0xff);
    for (PrivatePort privatePort : ports) {
      PortHandler portData = privatePort.port;
      if (portData.isReader() && (callbackInfo.port & portData.getMask()) == portData.getValue()) {
        byte[] attached = new byte[]{0};
        byte value = portData.read(callbackInfo.port, attached);
        callbackInfo.value &= (byte) (value | callbackInfo.attached);
        callbackInfo.attached |= attached[0] != 0 ? (byte) 0xff : 0;
      }
    }

    if (callbackInfo.attached != (byte) 0xff) {
      callbackInfo.value = mergeFloatingBus(callbackInfo.value, callbackInfo.attached,
          (byte) machine.get().unattachedPort());
    }

    if (Rzx.recording) {
      Rzx.storeByte(callbackInfo.value);
    }

    return callbackInfo.value;
  }

  // Merge the read value with the floating bus
  public byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
    return (byte) (value & (floatingBus | attached));
  }

  // Write a byte to a port, taking the appropriate time
  public void writePort(int port, byte b) {
    ula.contendPortEarly(port);
    writePortInternal(port, b);
    ula.contendPortLate(port);
    tStatesHolder.setTstates(tStatesHolder.getTstates() + 1);
  }

  // Write a byte to a port, taking no time
  public void writePortInternal(int port, byte b) {
    PeripheralData callbackInfo = new PeripheralData(port, (byte) 0, b);
    for (PrivatePort privatePort : ports) {
      PortHandler portData = privatePort.port;
      if (portData.isWriter() && (callbackInfo.port & portData.getMask()) == portData.getValue()) {
        portData.write(callbackInfo.port, callbackInfo.value);
      }
    }
  }

  // Update cartridge menu
//    private  void updateCartridgeMenu() {
//        boolean dock = (Machine.current.capabilities & Libspectrum.MachineCapability.TIMEX_DOCK) != 0;
//        boolean if2 = isActive(Type.INTERFACE2);
//        boolean cartridge = dock || if2;
//
//        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE, cartridge);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_DOCK, dock);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_IF2, if2);
//    }
//
//    // Update IDE menu
//    private  void updateIdeMenu() {
//        boolean simpleide = Settings.current.simpleideActive;
//        boolean zxatasp = Settings.current.zxataspActive;
//        boolean zxcf = Settings.current.zxcfActive;
//        boolean divide = Settings.current.divideEnabled;
//        boolean divmmc = Settings.current.divmmcEnabled;
//        boolean zxmmc = Settings.current.zxmmcEnabled;
//        boolean ide = simpleide || zxatasp || zxcf || divide || divmmc || zxmmc;
//
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE, ide);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_SIMPLE8BIT, simpleide);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXATASP, zxatasp);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXCF, zxcf);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_DIVIDE, divide);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_DIVMMC, divmmc);
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IDE_ZXMMC, zxmmc);
//    }

//    // Update peripherals status
//    private  void updatePeripheralsStatus() {
//        Ui.menuActivate(Ui.MenuItem.MEDIA_IF1, isActive(Type.INTERFACE1));
//        Ui.menuActivate(Ui.MenuItem.MEDIA_CARTRIDGE_IF2, isActive(Type.INTERFACE2));
//        updateCartridgeMenu();
//        updateIdeMenu();

  ////        If1.updateMenu();
  ////        Multiface.statusUpdate();
  ////        SpecPlus3.updateFdd();
//    }

//    // Disable optional peripherals
//    public  void disableOptional() {
//        if (Ui.mousePresent && Ui.mouseGrabbed) {
//            Ui.mouseGrabbed = Ui.mouseRelease(true);
//        }
//
//        peripherals.forEach((type, privatePeriph) -> {
//            if (privatePeriph.present == Present.NEVER || privatePeriph.present == Present.OPTIONAL) {
//                if (privatePeriph.peripheral.hasOption()) {
//                    privatePeriph.peripheral.getOption()[0] = false;
//                }
//            }
//        });
//
//        updatePeripheralsStatus();
//    }

  // Update peripherals and determine if a hard reset is needed
  public boolean update() {
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
        case OPTIONAL -> privatePeriph.peripheral.hasOption() && privatePeriph.peripheral.getOption()[0];
        case ALWAYS -> true;
      };
      boolean changed = activateType(privatePeriph.peripheral.getClass(), active);
      needsHardReset[0] |= changed && privatePeriph.peripheral.hasHardReset();
    });

//        updatePeripheralsStatus();
    machine.get().memoryMap();

    return needsHardReset[0];
  }

  // Perform post-update hook
  public void postHook() {
    if (update()) {
//      machine.reset(true);
    }
  }

  // Check if a hard reset is needed without activating/deactivating
  public boolean postCheck() {
    boolean[] needsHardReset = {false};

    peripherals.forEach((type, privatePeriph) -> {
      boolean active = switch (privatePeriph.present) {
        case NEVER -> false;
        case OPTIONAL -> privatePeriph.peripheral.hasOption() && privatePeriph.peripheral.getOption()[0];
        case ALWAYS -> true;
      };
      boolean needsReset = privatePeriph != null && privatePeriph.active != active && privatePeriph.peripheral.hasHardReset();
      needsHardReset[0] |= needsReset;
    });

    return needsHardReset[0];
  }
}
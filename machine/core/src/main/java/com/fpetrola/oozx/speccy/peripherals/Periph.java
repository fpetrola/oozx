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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.UserInterface;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import cern.colt.list.ObjectArrayList;
import com.fpetrola.oozx.MachineChangeListener;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.*;

@Singleton
public class Periph implements IPeriph {
  private Z80Clock z80Clock;
  private Settings settings;
  private SpectrumMachine spectrumMachine;
  private final UserInterface userInterface;

  @Inject
  public Periph(Z80Clock z80Clock, Settings settings, UserInterface userInterface) {
    this.userInterface = userInterface;
    this.z80Clock = z80Clock;
    this.settings = settings;
  }

  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }

  // Enum for peripheral types
  public enum Type {
    UNKNOWN,
    _128_MEMORY(Spec128MemoryPeripheral.class),
    // Naming the class is what makes marking the type present mean anything: without it the type
    // falls back to the generic peripheral, so "AY is always present" marked something that was
    // not the AY, and the real one sat registered and never activated.
    AY(AyPeripheral.class),
    AY_FULL_DECODE,
    AY_PLUS3(AyPlus3Peripheral.class),
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
    MELODIK(MelodikPeripheral.class),
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
  private Map<Class<? extends ZxPeripheral>, PrivatePeripheral> peripherals = new HashMap<>();

  // List of currently active ports
  private final ObjectArrayList ports = new ObjectArrayList();

  // Strings for debugger events
  private final String PAGE_EVENT_STRING = "page";
  private final String UNPAGE_EVENT_STRING = "unpage";

//  // Register a peripheral with the system
//  public  void register(Type type, Peripheral peripheral) {
//    ZxPeripheralAdapter peripheral1 = new ZxPeripheralAdapter(type, peripheral);
//
//    register(peripheral1);
//  }

  @Override
  public void register(ZxPeripheral zxPeripheral) {
    if (peripherals == null) {
      peripherals = new HashMap<>();
    }

    PrivatePeripheral privatePeriph = new PrivatePeripheral(Present.NEVER, false, zxPeripheral);
    peripherals.put(zxPeripheral.getClass(), privatePeriph);
  }

  // Set whether a peripheral can be present on this machine
  @Override
  public void setPresent(Type type, Present present) {
    Class<? extends ZxPeripheral> zxPeripheralClass = type.getZxPeripheralClass();
    setPresent(zxPeripheralClass, present);
  }

  @Override
  public void setPresent(Class<? extends ZxPeripheral> zxPeripheralClass, Present present) {
    PrivatePeripheral typeData = peripherals.get(zxPeripheralClass);
    if (typeData != null) {
      typeData.present = present;
    }
  }

  // Mark a specific peripheral as (in)active
  @Override
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
      privatePeriph.peripheral.deactivate();
      ObjectArrayList toRemove = new ObjectArrayList();
      for (int i = 0, portsSize = ports.size(); i < portsSize; i++) {
        PrivatePort p = (PrivatePort) ports.get(i);
        if (p.type == type) {
          toRemove.add(p);
        }
      }
      ports.removeAll(toRemove, true);
    }

    return true;
  }

  /** The registered peripheral of a kind, or null if this build has none. */
  public ZxPeripheral find(Type type) {
    PrivatePeripheral data = peripherals.get(type.getZxPeripheralClass());
    return data == null ? null : data.peripheral;
  }

  // Check if a specific peripheral is active
  @Override
  public boolean isActive(Type type) {
    // By the class, as everything else here does: the map is keyed by it, and a Type is a name
    // for one. Looking the Type up directly found nothing and answered no about every peripheral
    // there has ever been, including the ULA.
    PrivatePeripheral typeData = peripherals.get(type.getZxPeripheralClass());
    return typeData != null && typeData.active;
  }

  // Empty out the list of peripherals
  @Override
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
  @Override
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
  @Override
  public byte readPort(int port) {
    byte b = readPortInternal(port);

    // Special case for 128K/+2 machines
    if ((port & 0x8002) == 0 &&
        (getSpectrumMachine().getClass() == Spec128.class ||
            getSpectrumMachine().getClass() == SpecPlus2.class)) {
      writePortInternal(0x7ffd, b);
    }

    z80Clock.addTStates(1);
//        b= -1;
    return b;
  }

  private SpectrumMachine getSpectrumMachine() {
    return spectrumMachine;
  }

  // Read a byte from a port, taking no time
  private byte readPortInternal(int port) {
    // Handle RZX playback

    // Normal port read
    PeripheralData callbackInfo = new PeripheralData(port, (byte) 0x00, (byte) 0xff);
    for (int i = 0, portsSize = ports.size(); i < portsSize; i++) {
      PrivatePort privatePort = (PrivatePort) ports.get(i);
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
          (byte) getSpectrumMachine().unattachedPort(port));
    }

    return callbackInfo.value;
  }

  // Merge the read value with the floating bus
  @Override
  public byte mergeFloatingBus(byte value, byte attached, byte floatingBus) {
    return (byte) (value & (floatingBus | attached));
  }

  // Write a byte to a port, taking the appropriate time
  @Override
  public void writePort(int port, byte b) {
    writePortInternal(port, b);
    z80Clock.addTStates(1);
  }

  // Write a byte to a port, taking no time
  @Override
  public void writePortInternal(int port, byte b) {
    PeripheralData callbackInfo = new PeripheralData(port, (byte) 0, b);
    for (int i = 0, portsSize = ports.size(); i < portsSize; i++) {
      PrivatePort privatePort = (PrivatePort) ports.get(i);
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
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_CARTRIDGE, cartridge);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_CARTRIDGE_DOCK, dock);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_CARTRIDGE_IF2, if2);
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
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE, ide);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_SIMPLE8BIT, simpleide);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_ZXATASP, zxatasp);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_ZXCF, zxcf);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_DIVIDE, divide);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_DIVMMC, divmmc);
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IDE_ZXMMC, zxmmc);
//    }

//    // Update peripherals status
//    private  void updatePeripheralsStatus() {
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_IF1, isActive(Type.INTERFACE1));
//        userInterface.menuActivate(UserInterface.MenuItem.MEDIA_CARTRIDGE_IF2, isActive(Type.INTERFACE2));
//        updateCartridgeMenu();
//        updateIdeMenu();

  //    }

//    // Disable optional peripherals
//    public  void disableOptional() {
//        if (userInterface.isMousePresent() && userInterface.isMouseGrabbed()) {
//            userInterface.releaseMouse();
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

    if (userInterface.isMousePresent()) {
      if (settings.current.kempstonMouse) {
        if (!userInterface.isMouseGrabbed()) {
          userInterface.grabMouse();
        }
      } else {
        if (userInterface.isMouseGrabbed()) {
          userInterface.releaseMouse();
        }
      }
    }

    peripherals.forEach((type, privatePeriph) -> {
      boolean active = switch (privatePeriph.present) {
        case NEVER -> false;
        case OPTIONAL -> privatePeriph.peripheral.isWanted();
        case ALWAYS -> true;
      };
      boolean changed = activateType(privatePeriph.peripheral.getClass(), active);
      needsHardReset[0] |= changed && privatePeriph.peripheral.hasHardReset();
    });

//        updatePeripheralsStatus();
    getSpectrumMachine().memoryMap();

    return needsHardReset[0];
  }

  // Perform post-update hook
  @Override
  public void postHook() {
    if (update()) {
//      machine.reset(true);
    }
  }

  // Check if a hard reset is needed without activating/deactivating
  @Override
  public boolean postCheck() {
    boolean[] needsHardReset = {false};

    peripherals.forEach((type, privatePeriph) -> {
      boolean active = switch (privatePeriph.present) {
        case NEVER -> false;
        case OPTIONAL -> privatePeriph.peripheral.isWanted();
        case ALWAYS -> true;
      };
      boolean needsReset = privatePeriph != null && privatePeriph.active != active && privatePeriph.peripheral.hasHardReset();
      needsHardReset[0] |= needsReset;
    });

    return needsHardReset[0];
  }
}
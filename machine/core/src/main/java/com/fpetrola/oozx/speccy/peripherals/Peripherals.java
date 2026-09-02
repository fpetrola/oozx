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
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.ports.PortHandler;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.*;

@Singleton
public class Peripherals implements PeripheralBus {
  private Z80Clock z80Clock;
  private Settings settings;
  private SpectrumMachine spectrumMachine;
  private final UserInterface userInterface;

  @Inject
  public Peripherals(Z80Clock z80Clock, Settings settings, UserInterface userInterface) {
    this.userInterface = userInterface;
    this.z80Clock = z80Clock;
    this.settings = settings;
  }

  /**
   * Everything is switched off when the machine changes, and switched on again by the update that
   * follows: a device binds to the machine it was switched on for, so one left on across the
   * change would go on answering for the machine that is no longer there.
   */
  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
    clear();
  }

  // Private structure for peripheral data
  private class PrivatePeripheral {
    boolean active;
    Peripheral peripheral;

    PrivatePeripheral(Peripheral peripheral) {
      this.peripheral = peripheral;
    }
  }

  // Private structure for port response with peripheral type
  private class PrivatePort {
    Class<? extends Peripheral> type;
    PortHandler port;

    PrivatePort(Class<? extends Peripheral> type, PortHandler port) {
      this.type = type;
      this.port = port;
    }
  }

  // All peripherals known to the system
  private Map<Class<? extends Peripheral>, PrivatePeripheral> peripherals = new HashMap<>();

  // List of currently active ports
  private final ObjectArrayList ports = new ObjectArrayList();

  // The few of those that asked to hear reads of their own port, kept apart so a read costs
  // nothing on a machine where nobody listens.
  private final ObjectArrayList busListeners = new ObjectArrayList();

  // Strings for debugger events
  private final String PAGE_EVENT_STRING = "page";
  private final String UNPAGE_EVENT_STRING = "unpage";

  @Override
  public void register(Peripheral peripheral) {
    if (peripherals == null) {
      peripherals = new HashMap<>();
    }

    peripherals.put(peripheral.getClass(), new PrivatePeripheral(peripheral));
  }

  // Mark a specific peripheral as (in)active
  @Override
  public boolean activateType(Class<? extends Peripheral> type, boolean active) {
    PrivatePeripheral privatePeriph = peripherals.get(type);
    if (privatePeriph == null || privatePeriph.active == active) {
      return false;
    }

    privatePeriph.active = active;

    if (active) {
      privatePeriph.peripheral.activate(getSpectrumMachine());
      // The first to attach to a port wins the bits it drives, as in Fuse, so something plugged
      // in goes in front of the machine's own chips: a board on the even ports the ULA answers
      // has to be heard over the keyboard.
      int front = 0;
      for (PortHandler port : privatePeriph.peripheral.getPorts()) {
        PrivatePort privatePort = new PrivatePort(type, port);
        if (privatePeriph.peripheral instanceof Pluggable) {
          ports.beforeInsert(front++, privatePort);
        } else {
          ports.add(privatePort);
        }
        if (port.listensToBusReads()) busListeners.add(privatePort);
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
      busListeners.removeAll(toRemove, true);
    }

    return true;
  }

  /** The registered peripheral of a kind, or null if this build has none. */
  @Override
  public Peripheral find(Class<? extends Peripheral> peripheralClass) {
    PrivatePeripheral data = peripherals.get(peripheralClass);
    return data == null ? null : data.peripheral;
  }

  @Override
  public boolean isActive(Class<? extends Peripheral> peripheralClass) {
    PrivatePeripheral typeData = peripherals.get(peripheralClass);
    return typeData != null && typeData.active;
  }

  // Empty out the list of peripherals
  @Override
  public void clear() {
    ports.clear();
    busListeners.clear();
    if (peripherals != null) {
      peripherals.forEach((type, data) -> {
        // Told, not just marked: a device that was on has things to put back - a chip in the
        // mixer, a ROM paged over the machine's - and only it knows what they are.
        if (data.active) {
          data.peripheral.deactivate();
        }
        data.active = false;
      });
    }
  }

  // Clean up peripherals at the end of emulation
  @Override
  public void end() {
    ports.clear();
    busListeners.clear();
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

    for (int i = 0, size = busListeners.size(); i < size; i++) {
      PrivatePort listener = (PrivatePort) busListeners.get(i);
      if ((port & listener.port.getMask()) == listener.port.getValue()) {
        listener.port.busRead(port, b);
      }
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
      boolean changed = activateType(privatePeriph.peripheral.getClass(), wanted(privatePeriph));
      needsHardReset[0] |= changed && privatePeriph.peripheral.hasHardReset();
    });

//        updatePeripheralsStatus();
    getSpectrumMachine().memoryMap();

    return needsHardReset[0];
  }

  /** Switched on when it belongs on the machine that is running and whoever is here asked for it. */
  private boolean wanted(PrivatePeripheral privatePeriph) {
    return privatePeriph.peripheral.fitsOn(getSpectrumMachine()) && privatePeriph.peripheral.isWanted();
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
      boolean needsReset = privatePeriph.active != wanted(privatePeriph) && privatePeriph.peripheral.hasHardReset();
      needsHardReset[0] |= needsReset;
    });

    return needsHardReset[0];
  }
}
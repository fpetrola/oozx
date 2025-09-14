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

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.google.inject.Provider;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.ports.Backplane;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * The peripherals this build has, and which of them are switched on. Switching one on binds it
 * to the machine that is running and puts its ports on the bus; off takes them back.
 * <p>
 * Everything is switched off when the machine changes, and switched on again by the update that
 * follows: a device binds to the machine it was switched on for, so one left on across the
 * change would go on answering for the machine that is no longer there.
 */
@Singleton
public class PeripheralRegistry {
  /** A peripheral and whether it is switched on, which is not the peripheral's to know. */
  private static class Registration {
    boolean active;
    final Peripheral peripheral;

    Registration(Peripheral peripheral) {
      this.peripheral = peripheral;
    }
  }

  private final Map<Class<? extends Peripheral>, Registration> registry = new HashMap<>();
  private final Backplane backplane;
  private final MemoryBus memory;
  private final Supplier<Machine> machine;

  @Inject
  public PeripheralRegistry(Backplane backplane, MemoryBus memory, Provider<Machine> machine) {
    this.machine = Suppliers.memoize(machine::get);
    this.backplane = backplane;
    this.memory = memory;
  }

  public void register(Peripheral peripheral) {
    registry.put(peripheral.getClass(), new Registration(peripheral));
  }

  /** The registered peripheral of a kind, or null if this build has none. */
  public Peripheral find(Class<? extends Peripheral> type) {
    Registration device = registry.get(type);
    return device == null ? null : device.peripheral;
  }

  public boolean isActive(Class<? extends Peripheral> type) {
    Registration device = registry.get(type);
    return device != null && device.active;
  }

  /** Whether anything changed. */
  public boolean activateType(Class<? extends Peripheral> type, boolean active) {
    Registration device = registry.get(type);
    if (device == null || device.active == active) {
      return false;
    }
    device.active = active;

    if (active) {
      device.peripheral.activate(machine.get().current);
      backplane.attach(device.peripheral.getPorts(), device.peripheral instanceof Pluggable);
    } else {
      device.peripheral.deactivate();
      backplane.detach(device.peripheral.getPorts());
    }
    return true;
  }

  /** Switches on what belongs on that machine and has been asked for, off the rest; says whether that calls for a hard reset. */
  public boolean update() {
    boolean needsHardReset = false;
    for (Registration device : registry.values()) {
      boolean wanted = device.peripheral.fitsOn(machine.get().current) && device.peripheral.isWanted();
      needsHardReset |= activateType(device.peripheral.getClass(), wanted) && device.peripheral.hasHardReset();
    }
    machine.get().current.memoryMap();
    return needsHardReset;
  }

  /** The machine reset: every device that is switched on for it puts itself back. */
  public void machineWasReset(boolean hard) {
    registry.values().forEach(device -> {
      if (device.active) {
        device.peripheral.machineWasReset(hard);
      }
    });
  }

  /** Everything off. Told, not just marked: a device that was on has things to put back - a chip in the mixer, a ROM paged over the machine's - and only it knows what they are. */
  public void clear() {
    backplane.clear();
    memory.unplugAll();
    registry.values().forEach(device -> {
      if (device.active) {
        device.peripheral.deactivate();
      }
      device.active = false;
    });
  }

  public void end() {
    clear();
    registry.clear();
  }
}

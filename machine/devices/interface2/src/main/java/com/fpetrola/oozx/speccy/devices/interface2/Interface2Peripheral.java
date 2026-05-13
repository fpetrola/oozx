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
package com.fpetrola.oozx.speccy.devices.interface2;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * Interface 2 cartridge slot: its ROM replaces the machine's while inserted, and insert/eject
 * force a reset since the real hardware only reads the cartridge at power-on. No ports of its
 * own; its joystick sockets are handled as keyboard input.
 */
@Singleton
public class Interface2Peripheral extends PluggablePeripheral {

  private final MemoryBus memory;
  private final Machine machine;
  private final Rom rom;
  private final MappedMemory[] held;

  private Cartridge cartridge;
  private SpectrumMachine on;
  private boolean paged;

  @Inject
  public Interface2Peripheral(MemoryBus memory, Machine machine) {
    super(List.of());
    this.memory = memory;
    this.machine = machine;
    this.rom = new Rom(Cartridge.SIZE);
    held = new MappedMemory[]{new MappedMemory(0x0000, rom)};
  }

  /** Sinclair models with a /ROMCS edge connector, excluding the +2A and +3. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
  }

  @Override
  public void deactivate() {
    unpage();
    on = null;
  }

  /** Must run on the emulator's own thread; resets the machine to boot the cartridge. */
  public void insert(Cartridge cartridge) {
    this.cartridge = cartridge;
    machine.reset(false);
  }

  /** Must run on the emulator's own thread; resets the machine back to its own ROM. */
  public void eject() {
    cartridge = null;
    unpage();
    machine.reset(false);
  }

  public Cartridge cartridge() {
    return cartridge;
  }

  /** Loads the cartridge image and pages it in; only happens at reset, matching real hardware. */
  @Override
  public void machineWasReset(boolean hard) {
    paged = false;
    if (on == null || cartridge == null) {
      return;
    }
    rom.fill(cartridge.image());
    paged = true;
    memory.plug(held);
  }

  private void unpage() {
    paged = false;
    memory.unplug(held);
  }

}

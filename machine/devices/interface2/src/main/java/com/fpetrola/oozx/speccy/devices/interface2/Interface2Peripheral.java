/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

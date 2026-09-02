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

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * The Interface 2: a cartridge whose ROM takes the place of the machine's, from the moment it is
 * put in until it is taken out. Putting one in or taking it out resets the machine, which is what
 * the hardware forced you to do too - the cartridge only reads at power on.
 * <p>
 * No ports of its own: the two joystick sockets read as keys, which the keyboard already does.
 */
@Singleton
public class Interface2Peripheral extends PluggablePeripheral implements ZxModule, RomcsDevice {

  private final Memory memory;
  private final Module module;
  private final Machine machine;
  private final Settings settings;
  private final MemoryPage[] rom;

  private Cartridge cartridge;
  private SpectrumMachine on;
  private boolean paged;

  @Inject
  public Interface2Peripheral(Memory memory, Module module, Machine machine, Settings settings) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.machine = machine;
    this.settings = settings;
    this.rom = memory.newBank();
  }

  /** Any Sinclair machine with an edge connector that has /ROMCS, and the Timexes. Not a +2A or +3. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.has(MachineCapability.TIMEX_VIDEO)
        || !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    unpage();
    on = null;
  }

  /** On the emulator's thread: the machine restarts on the cartridge. */
  public void insert(Cartridge cartridge) {
    this.cartridge = cartridge;
    settings.current.if2File = cartridge.path();
    machine.reset(false);
  }

  /** On the emulator's thread: the machine restarts on its own ROM. */
  public void eject() {
    cartridge = null;
    settings.current.if2File = null;
    unpage();
    machine.reset(false);
  }

  public Cartridge cartridge() {
    return cartridge;
  }

  /** A reset is when the cartridge is read: its ROM goes over the machine's from here on. */
  @Override
  public void machineWasReset(boolean hard) {
    paged = false;
    if (on == null || cartridge == null) {
      return;
    }
    memory.fillRomBank(rom, 0, cartridge.image(), Cartridge.SIZE, true);
    on.getRamInfo().romcs = true;
    paged = true;
  }

  @Override
  public void mapRom() {
    if (paged) {
      memory.mapRomcsFull(rom);
    }
  }

  private void unpage() {
    if (paged && on != null) {
      on.getRamInfo().romcs = false;
    }
    paged = false;
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}

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
package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What every disk interface built round a Western Digital controller has: the controller and
 * its drives, a ROM paged over the machine's when the processor reaches the addresses that ROM
 * hooks, some RAM of its own beside it, and a button. The +D, the DISCiPLE, the Opus Discovery
 * and the Didaktik differ in the sizes, the ports and the hooks, and say so by overriding.
 */
public abstract class WdDiskInterface extends PluggablePeripheral implements ZxModule, RomcsDevice, DiskInterface {

  protected enum FdcRegister { STATUS_COMMAND, TRACK, SECTOR, DATA }

  protected final Memory memory;
  protected final Settings settings;
  protected final Cpu cpu;
  private final Module module;
  protected final MemoryPage[] rom;
  protected final MemoryPage[] ram;
  private final int romSize;
  protected final WdFdc fdc;
  protected final Fdd[] drives;
  private final List<PcTraps.Watch> hooks = new ArrayList<>();

  protected SpectrumMachine on;
  private boolean available;
  private boolean paged;

  protected WdDiskInterface(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                            Machine machine, WdFdc.Type type, int flags, int driveCount, int romSize, int ramSize) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    this.romSize = romSize;
    rom = memory.newRomBank();
    ram = ramSize > 0 ? memory.newRam(ramSize) : null;
    fdc = new WdFdc(type, 0, flags, events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed);
    drives = new Fdd[driveCount];
    for (int i = 0; i < driveCount; i++) {
      drives[i] = new Fdd(events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed, settings);
      drives[i].init(Fdd.Type.SHUGART, null, false);
    }
    fdc.currentDrive = drives[0];
    drives[0].select(true);
    fdc.dden = true;
  }

  /** The addresses in the machine's ROM that page this in when the processor reaches them. */
  protected abstract int[] hooks();

  /** Where the hooks are watched: before the fetch for most boards, after the instruction for the Opus. */
  protected PcTraps traps() {
    return cpu.beforeFetch();
  }

  protected abstract String defaultRomName();

  /** Whether a reset leaves it over the machine's ROM, or waiting for the first hook. */
  protected abstract boolean pagedAtReset();

  /** Its own state at a reset, after the shared part has been done. */
  protected abstract void reset(boolean hard);

  /** Which pages go where, while it is paged in. */
  protected abstract void mapPages();

  /** A blank disk of the shape this board's DOS formats. */
  protected abstract Disk blank() throws DiskException;

  /** One of the controller's registers on a port, with the bus told the port answered. */
  protected DefaultPortHandler fdcRegister(int port, FdcRegister which) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return (byte) fdcRead(which);
      }

      public void write(int port, byte value) {
        fdcWrite(which, value & 0xff);
      }
    };
  }

  protected int fdcRead(FdcRegister which) {
    return switch (which) {
      case STATUS_COMMAND -> fdc.srRead();
      case TRACK -> fdc.trRead();
      case SECTOR -> fdc.secRead();
      case DATA -> fdc.drRead();
    };
  }

  protected void fdcWrite(FdcRegister which, int value) {
    switch (which) {
      case STATUS_COMMAND -> fdc.crWrite(value);
      case TRACK -> fdc.trWrite(value);
      case SECTOR -> fdc.secWrite(value);
      case DATA -> fdc.drWrite(value);
    }
  }

  /** Puts that drive on the controller, both heads on that side, and carries the motor over. */
  protected void selectDrive(int drive, int side) {
    for (int i = 0; i < drives.length; i++) {
      drives[i].setHead(side);
      drives[i].select(drive == i);
    }
    if (fdc.currentDrive != drives[drive]) {
      if (fdc.currentDrive.motoron) {
        for (int i = 0; i < drives.length; i++) {
          drives[i].motorOn(drive == i);
        }
      }
      fdc.currentDrive = drives[drive];
    }
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
    for (int hook : hooks()) {
      hooks.add(traps().watch(hook, pc -> page()));
    }
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    hooks.forEach(PcTraps.Watch::off);
    hooks.clear();
    unpage();
    available = false;
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    paged = false;
    available = false;
    if (on == null) {
      return;
    }
    try {
      memory.loadRomBank(rom, 0, romName(), romSize, !romName().equals(defaultRomName()));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
    if (hard && ram != null) {
      Arrays.fill(ram[0].getPage(), (byte) 0);
    }
    fdc.masterReset();
    fdc.currentDrive = drives[0];
    drives[0].select(true);
    reset(hard);
    paged = true;
    if (pagedAtReset()) {
      on.getRamInfo().romcs = true;
    }
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  public boolean isPaged() {
    return paged && on != null && on.getRamInfo().romcs;
  }

  protected void page() {
    if (!available) {
      return;
    }
    paged = true;
    on.getRamInfo().romcs = true;
    on.memoryMap();
  }

  protected void unpage() {
    paged = false;
    if (on != null) {
      on.getRamInfo().romcs = false;
      on.memoryMap();
    }
  }

  @Override
  public void mapRom() {
    if (paged) {
      mapPages();
    }
  }

  @Override
  public int drives() {
    return drives.length;
  }

  @Override
  public Fdd drive(int which) {
    return drives[which];
  }

  public WdFdc fdc() {
    return fdc;
  }

  @Override
  public String buttonName() {
    return "NMI";
  }

  @Override
  public String buttonTip() {
    return "The button on the interface: stops the program and brings up its snapshot menu";
  }

  /** The button: an NMI, which the ROM answers with its snapshot menu. */
  @Override
  public void button() {
    cpu.nmi();
  }

  @Override
  public void insert(int which, Disk disk) {
    drives[which].insert(disk, false);
  }

  @Override
  public void insertBlank(int which) throws DiskException {
    drives[which].insert(blank(), false);
  }

  @Override
  public void eject(int which) {
    drives[which].eject();
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}

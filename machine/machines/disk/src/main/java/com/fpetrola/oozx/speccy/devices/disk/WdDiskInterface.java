/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common shape of a WD-based disk interface: controller, drives, a paged ROM triggered at
 * board-specific hook addresses, its own RAM, and a button. Size, ports and hooks are
 * board-specific and supplied by subclasses (+D, DISCiPLE, Opus Discovery, Didaktik).
 */
public abstract class WdDiskInterface extends PluggablePeripheral implements DiskInterface {

  protected enum FdcRegister { STATUS_COMMAND, TRACK, SECTOR, DATA }

  protected final MemoryBus memory;
  protected final Fdd.Limits floppy;
  protected final Roms roms;
  protected final Cpu cpu;
  protected final Rom rom;
  protected final Ram ram;
  private MappedMemory[] plugged = new MappedMemory[0];
  private final int romSize;
  protected final WdFdc fdc;
  protected final Fdd[] drives;
  private final List<PcTraps.Watch> hooks = new ArrayList<>();

  protected SpectrumMachine on;
  private boolean available;
  private boolean paged;

  protected WdDiskInterface(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events,
                            Machine machine, WdFdc.Type type, int flags, int driveCount, int romSize, int ramSize) {
    super(List.of());
    this.memory = memory;
    this.cpu = cpu;
    this.floppy = floppy;
    this.roms = roms;
    this.romSize = romSize;
    rom = new Rom(romSize);
    ram = ramSize > 0 ? new Ram(ramSize) : null;
    fdc = new WdFdc(type, 0, flags, events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed());
    drives = new Fdd[driveCount];
    for (int i = 0; i < driveCount; i++) {
      drives[i] = new Fdd(events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed(), floppy);
      drives[i].init(Fdd.Type.SHUGART, null, false);
    }
    fdc.currentDrive = drives[0];
    drives[0].select(true);
    fdc.dden = true;
  }

  /** ROM addresses that trigger paging this board in. */
  protected abstract int[] hooks();

  /** Hook timing; overridden by the Opus to watch after the instruction rather than before fetch. */
  protected PcTraps traps() {
    return cpu.beforeFetch();
  }

  /** True if this board should start paged in immediately after reset. */
  protected abstract boolean pagedAtReset();

  /** Board-specific reset logic, run after the common reset steps. */
  protected abstract void reset(boolean hard);

  /** Creates a blank disk in this board's native DOS format. */
  protected abstract Disk blank() throws DiskException;

  /** Wires one FDC register to a port, always claiming the bus answered. */
  protected Wired fdcRegister(int port, FdcRegister which) {
    return Wired.at(0x00ff, port, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(fdcRead(which));
      }

      public void write(int port, byte value) {
        fdcWrite(which, value & 0xff);
      }
    });
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

  /** Selects a drive and side on the controller, preserving current motor state. */
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
    for (int hook : hooks()) {
      hooks.add(traps().watch(hook, pc -> page()));
    }
  }

  @Override
  public void deactivate() {
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
      rom.fill(roms.of(this, romSize));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
    if (hard && ram != null) {
      Arrays.fill(ram.bytes, (byte) 0);
    }
    fdc.masterReset();
    fdc.currentDrive = drives[0];
    drives[0].select(true);
    reset(hard);
    paged = pagedAtReset();
    if (paged) {
      page();
    } else {
      unpage();
    }
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  public boolean isPaged() {
    return paged;
  }

  protected void page() {
    if (!available) {
      return;
    }
    paged = true;
    memory.unplug(plugged);
    plugged = ranges();
    memory.plug(plugged);
  }

  protected void unpage() {
    paged = false;
    memory.unplug(plugged);
    plugged = new MappedMemory[0];
  }

  /** Memory ranges this board currently exposes while paged in. */
  protected abstract MappedMemory[] ranges();

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

  /** Triggers an NMI, which the board's ROM handles by opening its snapshot menu. */
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

}

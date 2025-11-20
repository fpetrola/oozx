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
 * What every disk interface built round a Western Digital controller has: the controller and
 * its drives, a ROM paged over the machine's when the processor reaches the addresses that ROM
 * hooks, some RAM of its own beside it, and a button. The +D, the DISCiPLE, the Opus Discovery
 * and the Didaktik differ in the sizes, the ports and the hooks, and say so by overriding.
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

  /** The addresses in the machine's ROM that page this in when the processor reaches them. */
  protected abstract int[] hooks();

  /** Where the hooks are watched: before the fetch for most boards, after the instruction for the Opus. */
  protected PcTraps traps() {
    return cpu.beforeFetch();
  }

  /** Whether a reset leaves it over the machine's ROM, or waiting for the first hook. */
  protected abstract boolean pagedAtReset();

  /** Its own state at a reset, after the shared part has been done. */
  protected abstract void reset(boolean hard);

  /** A blank disk of the shape this board's DOS formats. */
  protected abstract Disk blank() throws DiskException;

  /** One of the controller's registers on a port, with the bus told the port answered. */
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

  /** What it puts on the bus while paged in, as laid out right now. */
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

}

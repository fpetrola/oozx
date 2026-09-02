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
package com.fpetrola.oozx.speccy.devices.plusd;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.DiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
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
 * What Miles Gordon Technology's two interfaces have in common, which is nearly everything: a
 * WD1770 with two Shugart drives, 8K of ROM and 8K of RAM paged over the machine's bottom 16K
 * when the processor reaches one of the addresses the ROM hooks or reads a "patch" port, a
 * printer port, and a button. The +D and the DISCiPLE differ in the port numbers, in what the
 * control register's bits mean, and in whether the halves can swap.
 */
public abstract class MgtDiskInterface extends PluggablePeripheral implements ZxModule, RomcsDevice, DiskInterface {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;
  public static final int DRIVES = 2;

  protected enum FdcRegister { STATUS_COMMAND, TRACK, SECTOR, DATA }

  protected final Memory memory;
  protected final Settings settings;
  private final Module module;
  private final Cpu cpu;
  private final ParallelPrinterPeripheral printer;
  protected final MemoryPage[] rom;
  protected final MemoryPage[] ram;
  protected final WdFdc fdc;
  protected final Fdd[] drives = new Fdd[DRIVES];
  private final List<PcTraps.Watch> hooks = new ArrayList<>();

  protected SpectrumMachine on;
  private boolean available;
  private boolean paged;
  protected int controlRegister;

  protected MgtDiskInterface(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                             Machine machine, ParallelPrinterPeripheral printer) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    this.printer = printer;
    rom = memory.newRomBank();
    ram = memory.newRam(RAM_SIZE);
    fdc = new WdFdc(WdFdc.Type.WD1770, 0, WdFdc.FLAG_NONE, events, cpu.getClock(),
        () -> machine.current.getTimings().processorSpeed);
    for (int i = 0; i < DRIVES; i++) {
      drives[i] = new Fdd(events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed, settings);
      drives[i].init(Fdd.Type.SHUGART, null, false);
    }
    fdc.currentDrive = drives[0];
    drives[0].select(true);
    fdc.dden = true;
  }

  /** The addresses in the machine's ROM that page this in when the processor reaches them. */
  protected abstract int[] hooks();

  /** Which ROM file this reads at a reset, from the settings. */
  public abstract String romName();

  protected abstract String defaultRomName();

  /** Whether a reset leaves it over the machine's ROM, or waiting for the first hook. */
  protected abstract boolean pagedAtReset();

  /** Its own state at a reset, after the shared part has been done. */
  protected abstract void reset(boolean hard);

  /** Which 8K goes where, while it is paged in. */
  protected abstract void mapPages();

  /** The control register: drive, side, the printer's strobe, and whatever else the board has. */
  protected abstract void control(int b);

  /** One of the WD1770's registers on a port, with the bus told the port answered. */
  protected DefaultPortHandler fdcRegister(int port, FdcRegister which) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return (byte) switch (which) {
          case STATUS_COMMAND -> fdc.srRead();
          case TRACK -> fdc.trRead();
          case SECTOR -> fdc.secRead();
          case DATA -> fdc.drRead();
        };
      }

      public void write(int port, byte value) {
        switch (which) {
          case STATUS_COMMAND -> fdc.crWrite(value & 0xff);
          case TRACK -> fdc.trWrite(value & 0xff);
          case SECTOR -> fdc.secWrite(value & 0xff);
          case DATA -> fdc.drWrite(value & 0xff);
        }
      }
    };
  }

  /** The port whose read pages the interface in and whose write pages it out. */
  protected DefaultPortHandler patchPort(int port) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        page();
        return 0;
      }

      public void write(int port, byte value) {
        unpage();
      }
    };
  }

  /** The port the printer's data goes out on. */
  protected DefaultPortHandler printerDataPort(int port) {
    return new DefaultPortHandler(0x00ff, port, false, true) {
      public void write(int port, byte value) {
        printer.printer().write(value);
      }
    };
  }

  protected boolean printerAttached() {
    return printer.isWanted();
  }

  protected void strobe(boolean on) {
    printer.printer().strobe(on);
  }

  /** Puts that drive on the controller, both heads on that side, and carries the motor over. */
  protected void selectDrive(int drive, int side) {
    for (int i = 0; i < DRIVES; i++) {
      drives[i].setHead(side);
      drives[i].select(drive == i);
    }
    if (fdc.currentDrive != drives[drive]) {
      if (fdc.currentDrive.motoron) {
        for (int i = 0; i < DRIVES; i++) {
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
      hooks.add(cpu.beforeFetch().watch(hook, pc -> page()));
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
      memory.loadRomBank(rom, 0, romName(), ROM_SIZE, !romName().equals(defaultRomName()));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
    if (hard) {
      Arrays.fill(ram[0].getPage(), 0);
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

  public boolean isAvailable() {
    return available;
  }

  /** Whether its ROM is over the machine's right now. */
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

  public int controlRegister() {
    return controlRegister;
  }

  @Override
  public int drives() {
    return DRIVES;
  }

  @Override
  public Fdd drive(int which) {
    return drives[which];
  }

  @Override
  public String buttonName() {
    return "NMI";
  }

  @Override
  public String buttonTip() {
    return "The button on the interface: stops the program and brings up its snapshot menu";
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"mgt", "img", "dsk"};
  }

  public WdFdc fdc() {
    return fdc;
  }

  /** The button on the interface: an NMI, which its ROM answers with the snapshot menu. */
  public void button() {
    cpu.nmi();
  }

  public void insert(int which, Disk disk) {
    drives[which].insert(disk, false);
  }

  /** A blank double-sided 80-track disk, for the DOS to format. */
  public void insertBlank(int which) throws DiskException {
    drives[which].insert(Disk.blank(2, 80, Disk.Density.DD, Disk.Type.MGT), false);
  }

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

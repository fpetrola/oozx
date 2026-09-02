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
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The +D disk interface: 8K of ROM and 8K of RAM paged over the machine's bottom 16K, a WD1770
 * on ports 0xe3/0xeb/0xf3/0xfb, two Shugart drives, a printer port and a button.
 * <p>
 * It pages itself in when the processor reaches one of the addresses its ROM hooks - the error
 * restart, the NMI, and the keyboard scan the ROM calls every interrupt - and by a read of port
 * 0xe7; a write to 0xe7 pages it out. Ported from Fuse's plusd.c.
 */
@Singleton
public class PlusDPeripheral extends PluggablePeripheral implements ZxModule, RomcsDevice {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x2000;
  public static final int DRIVES = 2;
  private static final int[] HOOKS = {0x0008, 0x003a, 0x0066, 0x028e};

  private final Memory memory;
  private final Module module;
  private final Cpu cpu;
  private final Settings settings;
  private final ParallelPrinterPeripheral printer;
  private final MemoryPage[] rom;
  private final MemoryPage[] ram;
  private final WdFdc fdc;
  private final Fdd[] drives = new Fdd[DRIVES];
  private final List<PcTraps.Watch> hooks = new ArrayList<>();

  private SpectrumMachine on;
  private boolean available;
  private boolean paged;
  private int controlRegister;

  @Inject
  public PlusDPeripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
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
    ports(register(0xe3, () -> fdc.srRead(), fdc::crWrite),
        register(0xeb, () -> fdc.trRead(), fdc::trWrite),
        register(0xf3, () -> fdc.secRead(), fdc::secWrite),
        register(0xfb, () -> fdc.drRead(), fdc::drWrite),
        new DefaultPortHandler(0x00ff, 0x00ef, false, true) {
          public void write(int port, byte value) {
            control(value & 0xff);
          }
        },
        new DefaultPortHandler(0x00ff, 0x00e7, true, true) {
          public byte read(int port, byte[] attached) {
            page();
            return 0;
          }

          public void write(int port, byte value) {
            unpage();
          }
        },
        new DefaultPortHandler(0x00ff, 0x00f7, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) (printer.isWanted() ? 0x7f : 0xff);
          }

          public void write(int port, byte value) {
            printer.printer().write(value);
          }
        });
  }

  private interface Reader {
    int read();
  }

  private interface Writer {
    void write(int value);
  }

  private static DefaultPortHandler register(int port, Reader reader, Writer writer) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return (byte) reader.read();
      }

      public void write(int port, byte value) {
        writer.write(value & 0xff);
      }
    };
  }

  /** The Sinclair machines with an edge connector that has /ROMCS: not a +2A or +3, not a clone. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
    for (int hook : HOOKS) {
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
      memory.loadRomBank(rom, 0, settings.current.romPlusd, ROM_SIZE,
          !settings.current.romPlusd.equals(settings.defaults.romPlusd));
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
    // Fuse leaves it active but not paged after a reset: the first hook pages it in.
    paged = true;
  }

  public boolean isAvailable() {
    return available;
  }

  /** Whether its ROM is over the machine's right now. */
  public boolean isPaged() {
    return paged && on != null && on.getRamInfo().romcs;
  }

  private void page() {
    if (!available) {
      return;
    }
    paged = true;
    on.getRamInfo().romcs = true;
    on.memoryMap();
  }

  private void unpage() {
    paged = false;
    if (on != null) {
      on.getRamInfo().romcs = false;
      on.memoryMap();
    }
  }

  @Override
  public void mapRom() {
    if (paged) {
      memory.mapRomcs8k(0x0000, rom);
      memory.mapRomcs8k(0x2000, ram);
    }
  }

  /** Bits 0-1 which drive (only 2 is the second), bit 7 the side, bit 6 the printer's strobe. */
  private void control(int b) {
    controlRegister = b;
    int drive = (b & 0x03) == 2 ? 1 : 0;
    int side = (b & 0x80) != 0 ? 1 : 0;
    for (Fdd d : drives) {
      d.setHead(side);
    }
    drives[1 - drive].select(false);
    drives[drive].select(true);
    if (fdc.currentDrive != drives[drive]) {
      if (fdc.currentDrive.motoron) {
        drives[1 - drive].motorOn(false);
        drives[drive].motorOn(true);
      }
      fdc.currentDrive = drives[drive];
    }
    printer.printer().strobe((b & 0x40) != 0);
  }

  public int controlRegister() {
    return controlRegister;
  }

  public Fdd drive(int which) {
    return drives[which];
  }

  public WdFdc fdc() {
    return fdc;
  }

  /** The button on the +D: an NMI, which its ROM answers with the snapshot menu. */
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

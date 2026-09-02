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
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * The Beta 128: an FD1793 with up to four drives, and the 16K TR-DOS ROM that takes the place of
 * the machine's 48 BASIC when the processor reaches 0x3d00 - the addresses TR-DOS's entry
 * points sit at in that ROM - and leaves again the moment it runs anything above 0x4000. Ported
 * from Fuse's beta.c.
 * <p>
 * This is the one built into a Pentagon, whose third ROM it is; the interface somebody plugged
 * into a 48K or a 128 is the same board, in the devices, with its own ROM file and its own way
 * of saying whether it is there.
 */
@Singleton
public class Beta128Peripheral extends PluggablePeripheral implements ZxModule, RomcsDevice, DiskInterface {

  public static final int ROM_SIZE = 0x4000;
  public static final int DRIVES = 4;

  protected final Settings settings;
  private final Memory memory;
  private final Module module;
  private final Cpu cpu;
  private final Machine machine;
  private final Joystick joystick;
  private final MemoryPage[] rom;
  protected final WdFdc fdc;
  private final Fdd[] drives = new Fdd[DRIVES];

  protected SpectrumMachine on;
  private boolean available;
  private boolean paged;
  private int pcMask = 0xff00;
  private int pcValue = 0x3d00;
  private int systemRegister;
  private PcTraps.Watch pageWatch;
  private PcTraps.Watch unpageWatch;
  private final Runnable onNmi = this::pageForNmi;

  @Inject
  public Beta128Peripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                           Machine machine, Joystick joystick) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    this.machine = machine;
    this.joystick = joystick;
    rom = memory.newRomBank();
    fdc = new WdFdc(WdFdc.Type.FD1793, 0, WdFdc.FLAG_BETA128, events, cpu.getClock(),
        () -> machine.current.getTimings().processorSpeed);
    for (int i = 0; i < DRIVES; i++) {
      drives[i] = new Fdd(events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed, settings);
      drives[i].init(Fdd.Type.SHUGART, null, false);
    }
    fdc.currentDrive = null;
    selectDrive(0);
    fdc.dden = true;
    ports(new DefaultPortHandler(0x00ff, 0x001f, true, true) {
          public byte read(int port, byte[] attached) {
            if (paged) {
              attached[0] = (byte) 0xff;
              return (byte) fdc.srRead();
            }
            // The Pentagon's Kempston shares the port: it answers while TR-DOS is not paged.
            if (builtIn() && settings.current.joyKempston) {
              return joystick.kempstonRead(port, attached);
            }
            return (byte) 0xff;
          }

          public void write(int port, byte value) {
            if (paged) fdc.crWrite(value & 0xff);
          }
        },
        register(0x3f, FdcRegister.TRACK),
        register(0x5f, FdcRegister.SECTOR),
        register(0x7f, FdcRegister.DATA),
        new DefaultPortHandler(0x00ff, 0x00ff, true, true) {
          public byte read(int port, byte[] attached) {
            if (!paged) {
              return (byte) 0xff;
            }
            attached[0] = (byte) 0xff;
            return (byte) ((fdc.intrq ? 0x80 : 0) | (fdc.datarq ? 0x40 : 0));
          }

          public void write(int port, byte value) {
            if (paged) system(value & 0xff);
          }
        });
  }

  private enum FdcRegister { TRACK, SECTOR, DATA }

  private DefaultPortHandler register(int port, FdcRegister which) {
    return new DefaultPortHandler(0x00ff, port, true, true) {
      public byte read(int port, byte[] attached) {
        if (!paged) {
          return (byte) 0xff;
        }
        attached[0] = (byte) 0xff;
        return (byte) switch (which) {
          case TRACK -> fdc.trRead();
          case SECTOR -> fdc.secRead();
          case DATA -> fdc.drRead();
        };
      }

      public void write(int port, byte value) {
        if (!paged) {
          return;
        }
        switch (which) {
          case TRACK -> fdc.trWrite(value & 0xff);
          case SECTOR -> fdc.secWrite(value & 0xff);
          case DATA -> fdc.drWrite(value & 0xff);
        }
      }
    };
  }

  /** Bits 0-1 the drive, bit 3 HLT, bit 4 the side (the other way up), bit 5 MFM. */
  private void system(int b) {
    selectDrive(b & 0x03);
    fdc.setHlt((b & 0x08) != 0);
    fdc.currentDrive.setHead((b & 0x10) != 0 ? 0 : 1);
    fdc.dden = (b & 0x20) != 0;
    systemRegister = b;
  }

  public int systemRegister() {
    return systemRegister;
  }

  private void selectDrive(int which) {
    Fdd drive = drives[which & 0x03];
    if (fdc.currentDrive != drive) {
      if (fdc.currentDrive != null) {
        fdc.currentDrive.select(false);
      }
      fdc.currentDrive = drive;
      drive.select(true);
    }
  }

  /** Built into the machine, which is the Pentagon: the third ROM is TR-DOS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.has(MachineCapability.TRDOS_DISK);
  }

  /** What a machine comes with is always there. */
  @Override
  public boolean isWanted() {
    return true;
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  protected boolean builtIn() {
    return on != null && on.has(MachineCapability.TRDOS_DISK);
  }

  @Override
  public String romName() {
    return settings.current.romPentagon2;
  }

  protected String defaultRomName() {
    return settings.defaults.romPentagon2;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
    cpu.onNmi(onNmi);
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    cpu.offNmi(onNmi);
    disarm();
    unpage();
    available = false;
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    disarm();
    paged = false;
    available = false;
    if (on == null) {
      return;
    }
    pcMask = 0xff00;
    pcValue = 0x3d00;
    fdc.masterReset();
    try {
      memory.loadRomBank(rom, 0, romName(), ROM_SIZE, !romName().equals(defaultRomName()));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
    if (builtIn()) {
      // A Pentagon boots into TR-DOS.
      paged = true;
      on.getRamInfo().romcs = true;
    } else if (!on.has(MachineCapability.MEMORY_128)) {
      pcMask = 0xfe00;
      pcValue = 0x3c00;
      // On a 48K the system switch decides whether it boots into TR-DOS or waits to be called.
      if (settings.current.beta12848Boot) {
        paged = true;
        on.getRamInfo().romcs = true;
      }
    }
    selectDrive(0);
    arm();
  }

  /** The two watches on the bus: into TR-DOS at its entry points, out of it above the ROM. */
  private void arm() {
    pageWatch = cpu.beforeFetch().watch(pcValue, pcValue | ~pcMask & 0xffff, pc -> {
      if (!paged && in48Rom()) page();
    });
    unpageWatch = cpu.beforeFetch().watch(0x4000, 0xffff, pc -> {
      if (paged && in48Rom()) unpage();
    });
  }

  private void disarm() {
    if (pageWatch != null) {
      pageWatch.off();
      unpageWatch.off();
      pageWatch = null;
      unpageWatch = null;
    }
  }

  /** TR-DOS lives beside the 48 BASIC: on a 128 it is only reached with ROM 1 at the bottom. */
  private boolean in48Rom() {
    return !on.has(MachineCapability.MEMORY_128) || on.getRamInfo().currentRom() != 0;
  }

  private void pageForNmi() {
    if (available) {
      page();
    }
  }

  private void page() {
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
      memory.mapRomcsFull(rom);
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

  public WdFdc fdc() {
    return fdc;
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
  public void insert(int which, Disk disk) {
    if (settings.current.autoLoad && disk.type == Disk.Type.TRD) {
      disk.insertTrdosBootLoader();
    }
    drives[which].insert(disk, false);
  }

  @Override
  public void insertBlank(int which) throws DiskException {
    drives[which].insert(Disk.blank(2, 80, Disk.Density.DD, Disk.Type.TRD), false);
  }

  @Override
  public void eject(int which) {
    drives[which].eject();
  }

  @Override
  public String buttonName() {
    return "Boot";
  }

  @Override
  public String buttonTip() {
    return "Reset the machine into TR-DOS, with the 48 BASIC underneath, and boot from drive A";
  }

  /** Fuse's autoload: a reset, the 48 ROM at the bottom, and TR-DOS paged in over it. */
  @Override
  public void button() {
    machine.reset(true);
    if (on == null || !available) {
      return;
    }
    if (on.has(MachineCapability.MEMORY_128) || !settings.current.beta12848Boot) {
      cpu.jump(0);
      on.getRamInfo().lastByte |= 0x10;
      page();
    }
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"trd", "scl"};
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}

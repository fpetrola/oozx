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
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import com.fpetrola.oozx.speccy.modules.input.Input;

/**
 * The Beta 128: an FD1793 with up to four drives, and the 16K TR-DOS ROM that takes the place of
 * the machine's 48 BASIC when the processor reaches 0x3d00 - the addresses TR-DOS's entry
 * points sit at in that ROM - and leaves again the moment it runs anything above 0x4000.
 * <p>
 * This is the one built into a Pentagon, whose third ROM it is; the interface somebody plugged
 * into a 48K or a 128 is the same board, in the devices, with its own ROM file and its own way
 * of saying whether it is there.
 */
@Singleton
public class Beta128Peripheral extends PluggablePeripheral implements DiskInterface {
  private final Roms roms;
  private final Input.Setup input;

  public static final int ROM_SIZE = 0x4000;
  public static final int DRIVES = 4;

  public final TrDos trdos;
  private final Fdd.Limits floppy;
  private final MemoryBus memory;
  private final Cpu cpu;
  private final Machine machine;
  private final Joystick joystick;
  private final Rom rom;
  private final MappedMemory[] held;
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
  public Beta128Peripheral(MemoryBus memory, Cpu cpu, TrDos trdos, Fdd.Limits floppy, Scheduler events,
                           Machine machine, Joystick joystick, Roms roms, Input.Setup input) {
    super(List.of());
    this.roms = roms;
    this.input = input;
    this.memory = memory;
    this.cpu = cpu;
    this.trdos = trdos;
    this.floppy = floppy;
    this.machine = machine;
    this.joystick = joystick;
    rom = new Rom(ROM_SIZE);
    held = new MappedMemory[]{new MappedMemory(0x0000, rom)};
    fdc = new WdFdc(WdFdc.Type.FD1793, 0, WdFdc.FLAG_BETA128, events, cpu.getClock(),
        () -> machine.current.getTimings().processorSpeed());
    for (int i = 0; i < DRIVES; i++) {
      drives[i] = new Fdd(events, cpu.getClock(), () -> machine.current.getTimings().processorSpeed(), floppy);
      drives[i].init(Fdd.Type.SHUGART, null, false);
    }
    fdc.currentDrive = null;
    selectDrive(0);
    fdc.dden = true;
    ports(Wired.at(0x00ff, 0x001f, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            if (paged) {
              return BusAnswer.of(fdc.srRead());
            }
            // The Pentagon's Kempston shares the port: it answers while TR-DOS is not paged.
            if (builtIn() && input.kempstonJoystick) {
              return joystick.kempstonRead(port);
            }
            return BusAnswer.NONE;
          }

          public void write(int port, byte value) {
            if (paged) fdc.crWrite(value & 0xff);
          }
        }),
        register(0x3f, FdcRegister.TRACK),
        register(0x5f, FdcRegister.SECTOR),
        register(0x7f, FdcRegister.DATA),
        Wired.at(0x00ff, 0x00ff, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            if (!paged) {
              return BusAnswer.NONE;
            }
            return BusAnswer.of((fdc.intrq ? 0x80 : 0) | (fdc.datarq ? 0x40 : 0));
          }

          public void write(int port, byte value) {
            if (paged) system(value & 0xff);
          }
        }));
  }

  private enum FdcRegister { TRACK, SECTOR, DATA }

  private Wired register(int port, FdcRegister which) {
    return Wired.at(0x00ff, port, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        if (!paged) {
          return BusAnswer.NONE;
        }
        return BusAnswer.of(switch (which) {
          case TRACK -> fdc.trRead();
          case SECTOR -> fdc.secRead();
          case DATA -> fdc.drRead();
        });
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
    });
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
    return on != null && on.hasOnBoard(Beta128Peripheral.class);
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    cpu.onNmi(onNmi);
  }

  @Override
  public void deactivate() {
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
      rom.fill(roms.of(this, ROM_SIZE));
    } catch (RomNotLoadedException missing) {
      return;
    }
    available = true;
    if (builtIn()) {
      // A Pentagon boots into TR-DOS.
      page();
    } else if (!on.pagesThrough7ffd()) {
      pcMask = 0xfe00;
      pcValue = 0x3c00;
      // On a 48K the system switch decides whether it boots into TR-DOS or waits to be called.
      if (trdos.bootOn48k) {
        page();
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
    return !on.pagesThrough7ffd() || on.paging().rom() != 0;
  }

  private void pageForNmi() {
    if (available) {
      page();
    }
  }

  private void page() {
    paged = true;
    memory.plug(held);
  }

  private void unpage() {
    paged = false;
    memory.unplug(held);
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  public boolean isPaged() {
    return paged;
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
    if (trdos.autoBoot && disk.type == Disk.Type.TRD) {
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

  /** Autoload: a reset, the 48 ROM at the bottom, and TR-DOS paged in over it. */
  @Override
  public void button() {
    machine.reset(true);
    if (on == null || !available) {
      return;
    }
    if (on.pagesThrough7ffd() || !trdos.bootOn48k) {
      cpu.jump(0);
      on.paging().latch7ffd((byte) (on.paging().port7ffd() | 0x10));
      page();
    }
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"trd", "scl"};
  }

  /** The Beta 128 and its TR-DOS, whether it is on a Pentagon's board or plugged into a 48K. */
  @Singleton
  public static class TrDos {
    /** A 48K has no room for TR-DOS unless it is paged over the BASIC, which not every ROM survives. */
    public boolean bootOn48k;
    /** A disk put in the drive boots itself. */
    public boolean autoBoot;

    public boolean bootOn48k() {
      return bootOn48k;
    }

    public void setBootOn48k(boolean on) {
      bootOn48k = on;
    }

    public boolean autoBoot() {
      return autoBoot;
    }

    public void setAutoBoot(boolean on) {
      autoBoot = on;
    }
  }
}

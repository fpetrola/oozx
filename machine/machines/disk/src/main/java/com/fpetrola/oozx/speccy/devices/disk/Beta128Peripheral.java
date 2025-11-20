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
 * Beta 128 disk interface: an FD1793 with up to 4 drives and a 16K TR-DOS ROM that pages in over
 * 48 BASIC at 0x3d00-0x3dff (its entry points) and out again above 0x4000. Also the Pentagon's
 * built-in third ROM, same board and behaviour either way.
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
            // On a Pentagon this port is shared with Kempston, answered only when TR-DOS is unpaged.
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

  /** System register: bits 0-1 drive select, bit 3 HLT, bit 4 side (inverted), bit 5 density. */
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

  /** Built-in peripherals are never optional. */
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
      // Pentagon always pages TR-DOS in immediately after reset.
      page();
    } else if (!on.pagesThrough7ffd()) {
      pcMask = 0xfe00;
      pcValue = 0x3c00;
      // On a plugged-in 48K interface, autoboot is a user setting rather than automatic.
      if (trdos.bootOn48k) {
        page();
      }
    }
    selectDrive(0);
    arm();
  }

  /** Installs fetch watches that page TR-DOS in at its entry range and out above 0x4000. */
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

  /** True when the machine is currently running the 48-BASIC-compatible ROM (ROM 1 on a 128). */
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

  /** Resets the machine to the 48 BASIC ROM with TR-DOS paged in, then jumps to boot it. */
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

  /** User-configurable TR-DOS behaviour, shared across Pentagon and plug-in variants. */
  @Singleton
  public static class TrDos {
    /** Whether a 48K should page TR-DOS in at reset; not every third-party ROM tolerates this. */
    public boolean bootOn48k;
    /** Whether inserting a disk auto-runs its boot loader. */
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

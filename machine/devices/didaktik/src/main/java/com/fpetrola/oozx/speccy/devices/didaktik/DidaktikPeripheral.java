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
package com.fpetrola.oozx.speccy.devices.didaktik;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.WdDiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.machine.Roms;

/**
 * Didaktik 40/80: 14K ROM in three parts plus 2K RAM, a WD2797 on ports 0x81/0x83/0x85/0x87
 * (INTRQ/DRQ can raise /NMI per the AUX register), an unemulated 8255, and a SNAP button that
 * NMIs in and forces RST 0 at 0x0066. Pages in at 0x0000/0x0008, out at 0x1700.
 */
@Singleton
public class DidaktikPeripheral extends WdDiskInterface {

  public static final int ROM_SIZE = 0x3800;
  public static final int RAM_SIZE = 0x0800;
  public static final int DRIVES = 2;
  private static final int[] HOOKS = {0x0000, 0x0008};
  private static final int INTRQ_ENABLED = 0x80;
  private static final int DATARQ_ENABLED = 0x40;

  private int aux;
  private boolean snap;
  private PcTraps.Watch leaving;
  private PcTraps.Watch snapping;

  @Inject
  public DidaktikPeripheral(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events, Machine machine) {
    super(memory, cpu, floppy, roms, events, machine, WdFdc.Type.WD2797, WdFdc.FLAG_DRQ | WdFdc.FLAG_RDY,
        DRIVES, ROM_SIZE, RAM_SIZE);
    // This board drives READY itself; the FDC's own signal is unused.
    fdc.extraSignal = true;
    fdc.onIntrq = () -> {
      if ((aux & INTRQ_ENABLED) != 0) cpu.nmi();
    };
    fdc.onDatarq = () -> {
      if ((aux & DATARQ_ENABLED) != 0) cpu.nmi();
    };
    ports(fdcRegister(0x81, FdcRegister.STATUS_COMMAND),
        fdcRegister(0x83, FdcRegister.TRACK),
        fdcRegister(0x85, FdcRegister.SECTOR),
        fdcRegister(0x87, FdcRegister.DATA),
        Wired.at(0x0080, 0x0000, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            // Unemulated 8255: always reads as all ones, writes are ignored.
            return BusAnswer.of(0xff);
          }

          public void write(int port, byte value) {
          }
        }),
        Wired.at(0x00f9, 0x0089, new DefaultPortHandler(false, true) {
          public void write(int port, byte value) {
            auxWrite(value & 0xff);
          }
        }));
  }

  /** AUX register: bits 0-1 drive select, bits 2-3 motor on/off, bits 6-7 enable DRQ/INTRQ NMI. */
  private void auxWrite(int b) {
    if (((b ^ aux) & 0x01) != 0) drives[0].select((b & 0x01) != 0);
    if (((b ^ aux) & 0x02) != 0) drives[1].select((b & 0x02) != 0);
    fdc.currentDrive = drives[(b & 0x02) != 0 ? 1 : 0];
    if (((b ^ aux) & 0x04) != 0) drives[0].motorOn((b & 0x04) != 0);
    if (((b ^ aux) & 0x08) != 0) drives[1].motorOn((b & 0x08) != 0);
    aux = b;
  }

  public int aux() {
    return aux;
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    super.activate(machine);
    leaving = cpu.beforeFetch().watch(0x1700, pc -> {
      if (isPaged()) unpage();
    });
    snapping = cpu.beforeFetch().watch(0x0066, pc -> {
      if (snap && !isPaged()) {
        snap = false;
        // Forces RST 0, redirecting execution to 0x0000, a paging hook address.
        cpu.rst(0x0000);
        page();
      }
    });
  }

  @Override
  public void deactivate() {
    if (leaving != null) {
      leaving.off();
      snapping.off();
      leaving = null;
    }
    super.deactivate();
  }


  @Override
  protected boolean pagedAtReset() {
    return false;
  }

  @Override
  protected void reset(boolean hard) {
    aux = 0;
    snap = false;
    drives[1].select(false);
  }

  @Override
  protected MappedMemory[] ranges() {
    return new MappedMemory[]{new MappedMemory(0x0000, rom, 0, 0x2000), new MappedMemory(0x2000, rom, 0x2000, 0x1000),
        new MappedMemory(0x3000, rom, 0x3000, 0x800), new MappedMemory(0x3800, ram)};
  }

  @Override
  protected Disk blank() throws DiskException {
    return Disk.blank(2, 80, Disk.Density.DD, Disk.Type.D80);
  }

  @Override
  public String buttonName() {
    return "SNAP";
  }

  @Override
  public String buttonTip() {
    return "The SNAP button: an NMI that the Didaktik's ROM takes over, to save what is running";
  }

  /** Triggers NMI and arranges for the 0x0066 handler to force RST 0, paging this ROM in. */
  @Override
  public void button() {
    snap = true;
    cpu.nmi();
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"d80", "d40"};
  }

  /** Sold for machines with neither 128-style paging nor full port decoding (the 48K). */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough7ffd() && !machine.fullyDecodesPorts();
  }

}

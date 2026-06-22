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
package com.fpetrola.oozx.speccy.devices.opus;

import com.fpetrola.oozx.speccy.modules.memory.Registers;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.WdDiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.machine.Roms;

/**
 * Opus Discovery: 8K ROM, 2K RAM, then the WD1770 and a 6821 PIA both mapped into memory (WD1770
 * registers at 0x2800, PIA at 0x3000), no I/O ports of its own. Every DRQ raises /NMI. Pages in
 * at 0x0008/0x0048/0x1708, out at 0x1748 (after that instruction executes).
 */
@Singleton
public class OpusPeripheral extends WdDiskInterface {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x0800;
  public static final int DRIVES = 2;
  private static final int[] HOOKS = {0x0008, 0x0048, 0x1708};

  private final ParallelPrinterPeripheral printer;
  private final Registers controller;
  private final Registers pia;
  private PcTraps.Watch leaving;
  private int dataRegA, dataDirA, controlA, dataRegB, dataDirB, controlB;

  @Inject
  public OpusPeripheral(MemoryBus memory, Cpu cpu, Fdd.Limits floppy, Roms roms, Scheduler events,
                        Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, cpu, floppy, roms, events, machine, WdFdc.Type.WD1770, WdFdc.FLAG_DRQ, DRIVES, ROM_SIZE, RAM_SIZE);
    this.printer = printer;
    fdc.onDatarq = cpu::nmi;
    controller = new Registers(0x800) {
      public int read(int index) {
        return fdcRead(FdcRegister.values()[index & 0x03]);
      }

      public void write(int index, byte value) {
        fdcWrite(FdcRegister.values()[index & 0x03], value & 0xff);
      }
    };
    pia = new Registers(0x800) {
      public int read(int index) {
        return piaRead(index & 0x03);
      }

      public void write(int index, byte value) {
        piaWrite(index & 0x03, value & 0xff);
      }
    };
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }

  /** This board pages in after the opcode fetch completes, unlike the MGT-style boards. */
  @Override
  protected PcTraps traps() {
    return cpu.afterInstruction();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    super.activate(machine);
    leaving = cpu.afterInstruction().watch(0x1748, pc -> {
      if (isPaged()) unpage();
    });
  }

  @Override
  public void deactivate() {
    if (leaving != null) {
      leaving.off();
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
    dataRegA = dataDirA = controlA = dataRegB = dataDirB = controlB = 0;
  }

  @Override
  protected MappedMemory[] ranges() {
    return new MappedMemory[]{new MappedMemory(0x0000, rom), new MappedMemory(0x2000, ram),
        new MappedMemory(0x2800, controller), new MappedMemory(0x3000, pia)};
  }

  /** PIA register write: port A selects drive and side, port B drives the printer. */
  private void piaWrite(int reg, int data) {
    switch (reg) {
      case 0 -> {
        if ((controlA & 0x04) != 0) {
          dataRegA = data;
          selectDrive((data & 0x02) == 2 ? 1 : 0, (data & 0x10) != 0 ? 1 : 0);
        } else {
          dataDirA = data;
        }
      }
      case 1 -> controlA = data;
      case 2 -> {
        if ((controlB & 0x04) != 0) {
          dataRegB = data;
          printer.printer().write(data);
          // Sent immediately, since the busy line the ROM's strobe timing depends on is not modelled.
          printer.printer().strobe(false);
          printer.printer().strobe(true);
          printer.printer().strobe(false);
        } else {
          dataDirB = data;
        }
      }
      default -> controlB = data;
    }
  }

  private int piaRead(int reg) {
    return switch (reg) {
      case 0 -> (controlA & 0x04) != 0 ? (dataRegA &= ~0x40) : dataDirA;
      case 1 -> controlA | 0x40;
      case 2 -> (controlB & 0x04) != 0 ? dataRegB : dataDirB;
      default -> controlB;
    };
  }

  @Override
  protected Disk blank() throws DiskException {
    return Disk.blank(2, 80, Disk.Density.DD, Disk.Type.OPD);
  }

  @Override
  public String buttonName() {
    return null;
  }

  @Override
  public String buttonTip() {
    return null;
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"opd", "opu"};
  }

  /** Sinclair models whose edge connector exposes /ROMCS, required to fit this board. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

}

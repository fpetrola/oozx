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
package com.fpetrola.oozx.speccy.devices.opus;

import com.fpetrola.oozx.DevicePage;
import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.DiskException;
import com.fpetrola.oozx.speccy.devices.disk.WdDiskInterface;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The Opus Discovery: 8K of ROM at the bottom, 2K of RAM above it, and then the controller
 * and a 6821 PIA reached as memory - 0x2800 the WD1770's four registers, 0x3000 the PIA's - with
 * no ports of its own. Every DRQ pulls /NMI. It pages in at 0x0008, 0x0048 and 0x1708 and out at
 * 0x1748, after the instruction there. Ported from Fuse's opus.c.
 */
@Singleton
public class OpusPeripheral extends WdDiskInterface {

  public static final int ROM_SIZE = 0x2000;
  public static final int RAM_SIZE = 0x0800;
  public static final int DRIVES = 2;
  private static final int[] HOOKS = {0x0008, 0x0048, 0x1708};

  private final ParallelPrinterPeripheral printer;
  private final MemoryPage[] controller = new MemoryPage[1];
  private final MemoryPage[] pia = new MemoryPage[1];
  private PcTraps.Watch leaving;
  private int dataRegA, dataDirA, controlA, dataRegB, dataDirB, controlB;

  @Inject
  public OpusPeripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                        Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, module, cpu, settings, events, machine, WdFdc.Type.WD1770, WdFdc.FLAG_DRQ, DRIVES, ROM_SIZE, RAM_SIZE);
    this.printer = printer;
    fdc.onDatarq = cpu::nmi;
    controller[0] = new DevicePage() {
      public int get(int index) {
        return fdcRead(FdcRegister.values()[index & 0x03]);
      }

      public void set(int index, byte value) {
        fdcWrite(FdcRegister.values()[index & 0x03], value & 0xff);
      }
    };
    pia[0] = new DevicePage() {
      public int get(int index) {
        return piaRead(index & 0x03);
      }

      public void set(int index, byte value) {
        piaWrite(index & 0x03, value & 0xff);
      }
    };
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }

  /** The Opus looks after the opcode has been fetched, unlike the MGT boards. */
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
  public String romName() {
    return settings.current.romOpus;
  }

  @Override
  protected String defaultRomName() {
    return settings.defaults.romOpus;
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
  protected void mapPages() {
    memory.mapRomcs8k(0x0000, rom);
    memory.mapRomcs2k(0x2000, ram);
    memory.mapRomcs2k(0x2800, controller);
    memory.mapRomcs2k(0x3000, pia);
  }

  /** The 6821, as EightyOne reads it: port A is the drive and side, port B the printer. */
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
          // The ROM's own strobe timing is bound up with a busy line nobody emulates: sent now.
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

  /** The Sinclair machines with an edge connector that has /ROMCS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }
}

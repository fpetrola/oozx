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
package com.fpetrola.oozx.speccy.devices.didaktik;

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
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Arrays;

/**
 * The Didaktik 40/80: 14K of ROM in three pieces and 2K of RAM above them, a WD2797 on ports
 * 0x81/0x83/0x85/0x87 whose INTRQ and DRQ can pull /NMI when the AUX register allows, an 8255
 * Fuse does not emulate, and a SNAP button that takes an NMI into its ROM by turning the
 * instruction at 0x0066 into RST 0. Pages in at 0x0000 and 0x0008, out at 0x1700. Ported from
 * Fuse's didaktik.c.
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
  public DidaktikPeripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events, Machine machine) {
    super(memory, module, cpu, settings, events, machine, WdFdc.Type.WD2797, WdFdc.FLAG_DRQ | WdFdc.FLAG_RDY,
        DRIVES, ROM_SIZE, RAM_SIZE);
    // READY comes from the board and is always there, as in Fuse.
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
        new DefaultPortHandler(0x0080, 0x0000, true, true) {
          public byte read(int port, byte[] attached) {
            // The 8255: Fuse answers all ones and takes nothing.
            attached[0] = (byte) 0xff;
            return (byte) 0xff;
          }

          public void write(int port, byte value) {
          }
        },
        new DefaultPortHandler(0x00f9, 0x0089, false, true) {
          public void write(int port, byte value) {
            auxWrite(value & 0xff);
          }
        });
  }

  /** Bits 0-1 select the drives, 2-3 their motors, 6 and 7 let DRQ and INTRQ interrupt. */
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
        // RST 0 in place of the opcode there: the processor arrives at 0x0000, which is a hook.
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
  public String romName() {
    return settings.current.romDidaktik80;
  }

  @Override
  protected String defaultRomName() {
    return settings.defaults.romDidaktik80;
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
  protected void mapPages() {
    memory.mapRomcs8k(0x0000, rom);
    memory.mapRomcs4k(0x2000, Arrays.copyOfRange(rom, 4, 8));
    memory.mapRomcs2k(0x3000, Arrays.copyOfRange(rom, 6, 8));
    memory.mapRomcs2k(0x3800, ram);
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

  /** SNAP: an NMI, and the instruction the processor finds at 0x0066 becomes RST 0, which pages the ROM in. */
  @Override
  public void button() {
    snap = true;
    cpu.nmi();
  }

  @Override
  public String[] imageExtensions() {
    return new String[] {"d80", "d40"};
  }

  /** Sold for the 48K. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.MEMORY_128) && !machine.fullyDecodesPorts();
  }
}

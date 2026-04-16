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
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared DivIDE/DivMMC base: 8K EPROM, 8K RAM pages, control register at 0xe3 (bit 7 CONMEM
 * forces paging, bit 6 MAPRAM latches RAM page 3 over the EPROM until reset, low bits select
 * the 0x2000 page). The automapper pages in at ROM entry points and out at 0x1ff8-0x1fff, only
 * while write-protected or MAPRAM is set. The EPROM can be loaded from a file at hard reset.
 */
public abstract class DivPeripheral extends PluggablePeripheral implements IdeInterface {

  public static final int PAGE_SIZE = 0x2000;
  private static final int CONMEM = 0x80;
  private static final int MAPRAM = 0x40;
  private static final int[] ENTRIES = {0x0000, 0x0008, 0x0038, 0x0066, 0x04c6, 0x0562};

  protected final MemoryBus memory;
  private boolean writeProtect;
  protected final Cpu cpu;
  protected final Ram eprom;
  private final Ram[] ram;
  private MappedMemory[] plugged = new MappedMemory[0];
  private final List<PcTraps.Watch> watches = new ArrayList<>();

  protected SpectrumMachine on;
  private int control;
  private boolean active;
  private boolean automap;

  protected DivPeripheral(MemoryBus memory, Cpu cpu, Roms roms, int ramPages) {
    super(List.of());
    this.roms = roms;
    this.memory = memory;
    this.cpu = cpu;
    eprom = new Ram(PAGE_SIZE);
    ram = new Ram[ramPages];
    for (int i = 0; i < ramPages; i++) {
      ram[i] = new Ram(PAGE_SIZE);
      ram[i].pageNum = i;
    }
    Arrays.fill(eprom.bytes, (byte) 0xff);
  }

  /** EPROM image source file for a hard reset; null leaves it erased. */
  private final Roms roms;

  /** Jumper-on state: EPROM is readable but not writable. */
  public boolean writeProtect() {
    return writeProtect;
  }

  public void setWriteProtect(boolean writeProtect) {
    this.writeProtect = writeProtect;
  }

  protected Wired controlPort() {
    return Wired.at(0x00ff, 0x00e3, new DefaultPortHandler(false, true) {
      public void write(int port, byte value) {
        controlWrite(value & 0xff);
      }
    });
  }

  /** MAPRAM is a one-way latch: settable by a port write, never clearable that way. */
  public void controlWrite(int value) {
    control = value | control & MAPRAM;
    refresh();
  }

  public int control() {
    return control;
  }

  /** Automapper's page-in decision, independent of whether jumpers permit it to act. */
  public void setAutomap(boolean automap) {
    this.automap = automap;
    refresh();
  }

  /** Reevaluates paging after a jumper change, with register state unchanged. */
  public void refresh() {
    if (on == null) {
      return;
    }
    if ((control & CONMEM) != 0) {
      page();
    } else if (writeProtect() || (control & MAPRAM) != 0) {
      if (automap) page(); else unpage();
    } else {
      unpage();
    }
  }

  private void page() {
    active = true;
    protect();
    memory.unplug(plugged);
    plugged = new MappedMemory[]{new MappedMemory(0x0000, lower()), new MappedMemory(0x2000, upper())};
    memory.plug(plugged);
  }

  private void unpage() {
    active = false;
    memory.unplug(plugged);
    plugged = new MappedMemory[0];
  }

  private boolean mapram() {
    return (control & CONMEM) == 0 && (control & MAPRAM) != 0;
  }

  private Ram lower() {
    return mapram() ? ram[3] : eprom;
  }

  private Ram upper() {
    return ram[control & ram.length - 1];
  }

  /** Write eligibility: EPROM only under CONMEM; RAM page 3 never while substituting for it. */
  private void protect() {
    lower().writeProtected = (control & CONMEM) == 0 || writeProtect();
    upper().writeProtected = mapram() && upper() == ram[3];
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  /** Sinclair models whose edge connector exposes /ROMCS, required to fit this board. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    watches.add(cpu.beforeFetch().watch(0x3d00, 0x3dff, pc -> setAutomap(true)));
    watches.add(cpu.afterInstruction().watch(0x1ff8, 0x1fff, pc -> setAutomap(false)));
    for (int entry : ENTRIES) {
      watches.add(cpu.afterInstruction().watch(entry, pc -> setAutomap(true)));
    }
  }

  @Override
  public void deactivate() {
    watches.forEach(PcTraps.Watch::off);
    watches.clear();
    if (on != null) {
      unpage();
    }
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    active = false;
    if (on == null) {
      return;
    }
    if (hard) {
      control = 0;
      for (Ram page : ram) {
        Arrays.fill(page.bytes, (byte) 0);
      }
      Arrays.fill(eprom.bytes, (byte) 0xff);
      try {
        eprom.fill(roms.of(this, PAGE_SIZE));
      } catch (RomNotLoadedException missing) {
        // No EPROM loaded: reads as empty, same as with the write-protect jumper off.
        Arrays.fill(eprom.bytes, (byte) 0xff);
      }
    } else {
      control &= MAPRAM;
    }
    automap = false;
    refresh();
  }

  @Override
  public boolean isPaged() {
    return active;
  }

  @Override
  public String status() {
    return ((control & CONMEM) != 0 ? "CONMEM " : "") + ((control & MAPRAM) != 0 ? "MAPRAM " : "")
        + "page " + (control & ram.length - 1) + (writeProtect() ? ", EPROM protected" : ", EPROM writable");
  }

}

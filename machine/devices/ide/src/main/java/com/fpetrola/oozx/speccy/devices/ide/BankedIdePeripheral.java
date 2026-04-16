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

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

/**
 * Shared banked-flash logic for ZXATASP/ZXCF: 16K RAM banks stand in for flash, one paged over
 * low memory at a time. Upload mode routes writes to the bank while reads still see the
 * machine's ROM, so a bank can be filled without executing the half-written image. Bank
 * selection and read/write state come from each board's own register.
 */
public abstract class BankedIdePeripheral extends IdeBoard {

  public static final int BANK_SIZE = 0x4000;

  protected final MemoryBus memory;
  private final Ram ram;
  private final int banks;
  protected int bank;
  protected boolean paged;

  protected BankedIdePeripheral(MemoryBus memory, int banks, int units) {
    super(true, units);
    this.memory = memory;
    this.banks = banks;
    ram = new Ram(banks * BANK_SIZE);
  }

  /** Whether the currently paged-in bank accepts writes. */
  protected abstract boolean writable(int bank);

  /** True while in upload mode: reads pass through to ROM, writes still hit the bank. */
  protected abstract boolean upload();

  /** Sinclair models whose edge connector exposes /ROMCS, required to fit this board. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  /** Pages the selected bank into low memory, or unplugs it entirely. */
  protected void select(int bank, boolean on) {
    this.bank = bank & banks - 1;
    paged = on;
    ram.writeProtected = !writable(bank);
    memory.unplug(plugged);
    // Read-only during upload so the machine's own ROM stays visible for reads while writes fill the bank.
    plugged = paged ? new MappedMemory(0x0000, ram, this.bank * BANK_SIZE, BANK_SIZE, !upload(), true) : null;
    if (plugged != null) {
      memory.plug(plugged);
    }
  }

  private MappedMemory plugged;

  @Override
  public void machineWasReset(boolean hard) {
    super.machineWasReset(hard);
    if (on != null) {
      select(0, true);
    }
  }

  @Override
  public void deactivate() {
    if (on != null) {
      select(0, false);
    }
    super.deactivate();
  }

  public int bank() {
    return bank;
  }

  @Override
  public boolean isPaged() {
    return paged;
  }

  @Override
  public String status() {
    return (paged ? "bank " + bank + (writable(bank) ? ", writable" : ", protected") : "memory off")
        + (upload() ? ", uploading" : "");
  }
}

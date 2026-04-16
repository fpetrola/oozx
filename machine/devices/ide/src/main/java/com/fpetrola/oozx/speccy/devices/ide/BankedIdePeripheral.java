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
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;

/**
 * What the ZXATASP and the ZXCF add to a channel: banks of 16K that stand in for their flash,
 * one at a time over the bottom of the machine's memory, with an upload mode in which writes
 * land in the bank while reads still see the machine's ROM, so a bank can be filled without
 * running the half-written image. Which bank, whether it is on, and whether it can be written
 * come from each board's own register.
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

  /** Whether the bank at the bottom can be written just now. */
  protected abstract boolean writable(int bank);

  /** Whether reads go to the machine's ROM while writes go to the bank. */
  protected abstract boolean upload();

  /** The Sinclair machines with an edge connector that has /ROMCS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  /** Puts that bank at the bottom, or takes the board's memory away. */
  protected void select(int bank, boolean on) {
    this.bank = bank & banks - 1;
    paged = on;
    ram.writeProtected = !writable(bank);
    memory.unplug(plugged);
    // The selected bank at the bottom; while uploading it is written and not read, so the machine's ROM stays readable.
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

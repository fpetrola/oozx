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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.z80.registers.RegisterName;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * The fault this clone has, which is a thing that happens on its board and is in nothing it reads:
 * every interrupt it accepts leaves a byte of ones where the address of the next one is read from.
 * <p>
 * A part of the machine with no port of its own, so that it goes in when that machine goes in and
 * comes out when it comes out - which a fault fastened to the processor for good would not do.
 * <p>
 * The machine it was read off writes the byte as the interrupt is taken and before the address is
 * read; here it lands a moment later, so a table is eaten one interrupt further along and the
 * program comes apart just the same.
 */
@Singleton
public class InvesInterruptFault extends AbstractPeripheral {
  private final Cpu cpu;
  private final MemoryBus memory;
  private final Runnable itHappens = this::aByteOfOnes;

  @Inject
  public InvesInterruptFault(Cpu cpu, MemoryBus memory) {
    super(List.of());
    this.cpu = cpu;
    this.memory = memory;
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine instanceof Inves;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    cpu.stopTellingAboutInterrupts(itHappens);
    cpu.whenAnInterruptIsTaken(itHappens);
  }

  @Override
  public void deactivate() {
    cpu.stopTellingAboutInterrupts(itHappens);
  }

  private void aByteOfOnes() {
    var state = cpu.getOoz80().getState();
    int at = (state.getRegister(RegisterName.I).read() << 8) | (state.getRegister(RegisterName.R).read() & 0xff);
    memory.poke(at & 0xffff, (byte) 0xff);
  }
}

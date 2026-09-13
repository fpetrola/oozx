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

package com.fpetrola.oozx.speccy.devices.scld;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryPart;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Eight bits, one per eight K of the address space: where a bit is set, what the machine had there
 * is covered by the cartridge the machine carries or by the one behind it, and where it is clear
 * the machine's own memory shows through again.
 * <p>
 * Which of the two covers it is the top bit of the display register, since one chip decides both.
 */
@Singleton
public class TimexMemoryPeripheral extends AbstractPeripheral {
  /** Whether the cartridge behind the slot is the one being paged, rather than the one in it. */
  public static final int THE_OTHER_ONE = 0x80;

  private static final int CHUNKS = 8;
  private static final int CHUNK = 0x2000;

  private final MemoryBus memory;
  private final ScldPortHandler register;
  private final List<MappedMemory> plugged = new ArrayList<>();
  private MemoryPart[] slot = new MemoryPart[CHUNKS];
  private MemoryPart[] behind = new MemoryPart[CHUNKS];
  private int wanted;
  private int also;

  @Inject
  public TimexMemoryPeripheral(MemoryBus memory, ScldPortHandler register) {
    super(List.of());
    this.memory = memory;
    this.register = register;
    register.onWrite(this::map);
    ports(Wired.at(0x00ff, 0x00f4, new DefaultPortHandler(true, true) {
      @Override
      public void write(int port, byte value) {
        wanted = value & 0xff;
        map();
      }
    }));
  }

  /** What the machine has in its slot and behind it, eight K at a time, or nulls where it has none. */
  public void carrying(MemoryPart[] inTheSlot, MemoryPart[] behindIt) {
    slot = inTheSlot;
    behind = behindIt;
    map();
  }

  /** Which bits a machine wants covered beyond the ones the port asks for, when it has a rule of its own. */
  public void alsoCover(int chunks) {
    if (also == chunks) return;
    also = chunks;
    map();
  }

  public int wanted() {
    return wanted;
  }

  public void map() {
    memory.unplug(plugged.toArray(new MappedMemory[0]));
    plugged.clear();
    MemoryPart[] paged = (register.register() & THE_OTHER_ONE) != 0 ? behind : slot;
    for (int chunk = 0; chunk < CHUNKS; chunk++) {
      if (((wanted | also) & (1 << chunk)) == 0 || paged[chunk] == null) continue;
      plugged.add(new MappedMemory(chunk * CHUNK, paged[chunk], 0, CHUNK));
    }
    memory.plug(plugged.toArray(new MappedMemory[0]));
  }

  @Override
  public void activate(SpectrumMachine machine) {
    wanted = 0;
    map();
  }

  @Override
  public void deactivate() {
    wanted = 0;
    map();
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.onBoard().contains(TimexMemoryPeripheral.class);
  }
}

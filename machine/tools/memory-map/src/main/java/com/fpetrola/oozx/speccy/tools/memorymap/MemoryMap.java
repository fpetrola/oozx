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


package com.fpetrola.oozx.speccy.tools.memorymap;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;

import java.util.ArrayList;
import java.util.List;

/**
 * What is mapped where, right now.
 * <p>
 * Asked of the bus rather than worked out from the paging port: a 128K machine's banks, the
 * ROM of whatever machine it is, and anything a board has plugged over them all answer the same
 * question, and the bus is the one place that already knows the answer for every address. Which
 * is why nothing here knows what a machine model is, or that boards exist.
 */
public final class MemoryMap {

  /** The decode granularity of the bus: asking more finely than this cannot say anything more. */
  private static final int STEP = 0x800;

  /** A stretch of addresses that all answer the same: where it is, what it is, and how it behaves. */
  public record Region(int from, int to, String what, int page, boolean readable,
                       boolean writable, boolean contended) {
    public int size() {
      return to - from + 1;
    }
  }

  private MemoryMap() {
  }

  /**
   * The map as the bus sees it, run together: consecutive stretches answering the same thing
   * become one row, so a plain 48K is four rows and a board paged over the ROM shows as the
   * hole it makes.
   */
  public static List<Region> of(Speccy machine) {
    List<Region> regions = new ArrayList<>();
    for (int address = 0; address < 0x10000; address += STEP) {
      MappedMemory reading = machine.memory.reading(address);
      MappedMemory writing = machine.memory.writing(address);
      Region here = regionAt(address, reading, writing);
      Region before = regions.isEmpty() ? null : regions.get(regions.size() - 1);
      if (before != null && sameThing(before, here)) {
        regions.set(regions.size() - 1,
            new Region(before.from(), here.to(), before.what(), before.page(),
                before.readable(), before.writable(), before.contended()));
      } else {
        regions.add(here);
      }
    }
    return regions;
  }

  private static Region regionAt(int address, MappedMemory reading, MappedMemory writing) {
    if (reading == null) {
      return new Region(address, address + STEP - 1, "nothing", -1, false, false, false);
    }
    return new Region(address, address + STEP - 1,
        reading.memory().getClass().getSimpleName(), reading.memory().pageNum,
        reading.readable(),
        writing != null && writing.writable() && writing.memory().takesWrites(),
        reading.memory().contended);
  }

  private static boolean sameThing(Region before, Region here) {
    return before.what().equals(here.what()) && before.page() == here.page()
        && before.readable() == here.readable() && before.writable() == here.writable()
        && before.contended() == here.contended();
  }
}

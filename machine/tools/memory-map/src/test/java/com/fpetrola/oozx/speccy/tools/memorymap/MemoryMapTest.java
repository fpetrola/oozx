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
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryMapTest extends MachineTest {

  @Test
  void aPlainMachineIsItsRomAndItsRam() {
    Speccy speccy = silentMachine();

    List<MemoryMap.Region> map = MemoryMap.of(speccy);

    assertEquals(0x0000, map.get(0).from(), "it starts at the bottom");
    assertEquals(0xffff, map.get(map.size() - 1).to(), "and covers to the top");
    assertTrue(map.get(0).what().contains("Rom"), "the bottom 16K is ROM: " + map.get(0).what());
    assertEquals(0x3fff, map.get(0).to(), "all of it, run together into one row");
  }

  @Test
  void theRomCannotBeWrittenAndTheRamCan() {
    Speccy speccy = silentMachine();

    List<MemoryMap.Region> map = MemoryMap.of(speccy);

    assertTrue(map.get(0).readable(), "the ROM reads");
    assertEquals(false, map.get(0).writable(), "and does not take a write");
    MemoryMap.Region ram = map.stream().filter(region -> region.from() == 0x4000).findFirst()
        .orElseThrow();
    assertTrue(ram.writable(), "the screen's RAM takes a write");
  }

  /** The 16K the ULA shares with the processor, which is the one that costs a game its speed. */
  @Test
  void itSaysWhichRamIsContended() {
    Speccy speccy = silentMachine();

    MemoryMap.Region screen = MemoryMap.of(speccy).stream()
        .filter(region -> region.from() == 0x4000).findFirst().orElseThrow();

    assertTrue(screen.contended(), "the bank at 4000 is the contended one");
  }

  @Test
  void theWholeAddressSpaceIsAccountedFor() {
    Speccy speccy = silentMachine();

    int covered = MemoryMap.of(speccy).stream().mapToInt(MemoryMap.Region::size).sum();

    assertEquals(0x10000, covered, "every address belongs to exactly one row");
  }
}

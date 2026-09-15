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

package model.tests.devices;

import com.fpetrola.oozx.speccy.devices.spec256.Permutations;
import com.fpetrola.z80.memory.Memory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which pages a follower may look something up in with a number of its own: the ones that move
 * bits without mixing them, because only those mean the same thing done eight times as done once.
 */
class TablesThatMoveBitsTest {
  private static final int TABLE = 0xbe00;
  private final byte[] ram = new byte[0x10000];
  private final Memory machine = new Memory() {
    public int read(int address, int fetching) {
      return peek(address);
    }

    public int peek(int address) {
      return ram[address] & 0xff;
    }

    public void write(int address, int value) {
      ram[address] = (byte) value;
    }

    public void reset() {
    }
  };

  private void mirroring(int page) {
    for (int index = 0; index < 0x100; index++) {
      int reversed = 0;
      for (int bit = 0; bit < 8; bit++) if ((index & (1 << bit)) != 0) reversed |= 1 << (7 - bit);
      ram[page + index] = (byte) reversed;
    }
  }

  private boolean moveTheBitsAt(int address) {
    return new Permutations(machine).moveTheBitsAt(address);
  }

  @Test
  void aTableThatReversesTheEightBitsMovesThem() {
    mirroring(TABLE);
    assertTrue(moveTheBitsAt(TABLE), "every bit of an entry comes from one bit of its index");
    assertTrue(moveTheBitsAt(TABLE + 0x37), "and the question is about the page, not the byte");
  }

  @Test
  void oneEntryOutOfPlaceIsEnoughToSayNo() {
    mirroring(TABLE);
    ram[TABLE + 3] = 0;                                  // 0xc0 is what reversing 3 gives
    assertFalse(moveTheBitsAt(TABLE), "a table is checked against all 256 of its entries");
  }

  @Test
  void aPageThatSaysTheSameThingWhateverItIsAskedMovesNothing() {
    assertFalse(moveTheBitsAt(TABLE), "an empty page is not a table");
    for (int index = 0; index < 0x100; index++) ram[TABLE + index] = 0x5a;
    assertFalse(moveTheBitsAt(TABLE), "and neither is one that answers the same to everything");
  }

  @Test
  void aTableAGameWritesIsLookedAtAgain() {
    mirroring(TABLE);
    Permutations tables = new Permutations(machine);
    assertTrue(tables.moveTheBitsAt(TABLE));
    ram[TABLE + 3] = 0;
    assertTrue(tables.moveTheBitsAt(TABLE), "until it is told the page was written to");
    tables.written(TABLE + 3);
    assertFalse(tables.moveTheBitsAt(TABLE), "and then it answers about what is there now");
  }
}

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

package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.z80.memory.Memory;

/**
 * The pages of the machine's memory whose 256 bytes move bits without mixing them: a table where
 * every bit of an entry comes from one bit of the number that looked it up, which is what a game
 * uses to mirror or shift a byte. Renegade's is at 0xBE00 and reverses the eight bits.
 * <p>
 * Such a table is the one place where the eight followers are right to look something up each with
 * a number of its own. Moving bits commutes with taking a byte apart into planes: eight lookups
 * with eight indices land every pixel where one lookup would have landed it, and every pixel keeps
 * all eight bits of its colour. Any other table - a font, a sum - mixes bits belonging to different
 * pixels, and there a follower's own index is only a colour that walked into an address.
 * <p>
 * A game builds these tables at runtime, so the answer is forgotten when the page is written to.
 */
public final class Permutations {
  private static final int PAGE = 0x100;
  private final Memory machine;
  private final byte[] answered = new byte[PAGE];
  private final int[] from = new int[8];

  public Permutations(Memory machine) {
    this.machine = machine;
  }

  public boolean moveTheBitsAt(int address) {
    int page = address >> 8;
    if (answered[page] == 0) answered[page] = (byte) (theyDo(page << 8) ? 1 : 2);
    return answered[page] == 1;
  }

  /** A page a game has just written is a page whose answer was about what used to be there. */
  public void written(int address) {
    answered[(address >> 8) & 0xff] = 0;
  }

  /**
   * Which bit of the index each bit of an entry comes from, taken from the entries for zero and
   * for each single bit, and then checked against all 256: what passes is of that shape and no
   * other. A page that says the same thing whatever it is asked moves nothing and is not one.
   */
  private boolean theyDo(int page) {
    int constant = machine.peek(page);
    java.util.Arrays.fill(from, -1);
    boolean any = false;
    for (int bit = 0; bit < 8; bit++) {
      int moved = machine.peek(page | (1 << bit)) ^ constant;
      for (int out = 0; out < 8; out++) {
        if ((moved & (1 << out)) != 0) {
          if (from[out] >= 0) return false;
          from[out] = bit;
          any = true;
        }
      }
    }
    if (!any) return false;
    for (int index = 0; index < PAGE; index++) {
      int expected = constant;
      for (int out = 0; out < 8; out++) {
        if (from[out] >= 0 && (index & (1 << from[out])) != 0) expected ^= 1 << out;
      }
      if (machine.peek(page | index) != expected) return false;
    }
    return true;
  }
}

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


package com.fpetrola.oozx.speccy.tools.heatmap;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;

/**
 * How much code ran at each of the 64K addresses, and how much of that was just now.
 * <p>
 * Two counts per address rather than one: what it has ever run, which finds the code in a game
 * that has been playing for a while, and what it ran lately, which is the only one that shows a
 * routine stopping - a total never goes down, so on a total alone every loop the game ever
 * entered looks equally alive.
 */
public class Heat {

  /** Of what a place ran lately, what is left after a moment of not running: seven eighths. */
  private static final int KEEPS = 7;
  private static final int OF = 8;

  private final PcTraps.Watch watching;
  private final long[] ever = new long[0x10000];
  private final int[] lately = new int[0x10000];

  public Heat(Speccy machine) {
    watching = machine.cpu.beforeFetch().watch(0x0000, 0xffff, pc -> {
      ever[pc]++;
      // Held down rather than let grow: what matters is that it is running, not how hot it can
      // get, and a counter that runs away takes the whole picture's scale with it.
      lately[pc] = Math.min(lately[pc] + OF, 0xffff);
    });
  }

  public long ever(int address) {
    return ever[address & 0xffff];
  }

  public int lately(int address) {
    return lately[address & 0xffff];
  }

  /** A moment passed: what is not being run again goes cold. */
  public void fade() {
    for (int address = 0; address < lately.length; address++) {
      if (lately[address] != 0) {
        lately[address] = lately[address] * KEEPS / OF;
      }
    }
  }

  /** The hottest single address of either count, which is what the picture is scaled against. */
  public long hottest(boolean recently) {
    long most = 0;
    for (int address = 0; address < ever.length; address++) {
      most = Math.max(most, recently ? lately[address] : ever[address]);
    }
    return most;
  }

  /** How many of the 64K have ever run: a game's code is a small part of its memory. */
  public int placesThatRan() {
    int many = 0;
    for (long times : ever) {
      if (times != 0) {
        many++;
      }
    }
    return many;
  }

  public void forget() {
    java.util.Arrays.fill(ever, 0);
    java.util.Arrays.fill(lately, 0);
  }

  /** Lets the machine go: from here on it runs as though this had never been watching. */
  public void close() {
    watching.off();
  }
}

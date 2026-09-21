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


package com.fpetrola.oozx.speccy.tools.trace;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;

/**
 * The last instructions the machine ran, in the order it ran them.
 * <p>
 * A ring of addresses and nothing else: what the bytes there say is worked out when somebody
 * looks, not while the machine runs. A trace that disassembled as it went would cost more per
 * instruction than the instruction, and what is wanted at a breakpoint is the last few hundred
 * steps rather than every step of the afternoon.
 */
public class Trace {

  /** Long enough to hold what led to a crash, short enough to be nothing at all in memory. */
  public static final int KEPT = 1 << 14;

  private final PcTraps.Watch watching;
  private final int[] ran = new int[KEPT];
  private long steps;
  private boolean following = true;

  public Trace(Speccy machine) {
    watching = machine.cpu.beforeFetch().watch(0x0000, 0xffff, this::sawFetch);
  }

  private synchronized void sawFetch(int pc) {
    if (following) {
      ran[(int) (steps++ & KEPT - 1)] = pc;
    }
  }

  /** Whether it is still taking things down. Stopped, what it has stays as it was. */
  public synchronized void follow(boolean wanted) {
    following = wanted;
  }

  public synchronized boolean isFollowing() {
    return following;
  }

  public synchronized long steps() {
    return steps;
  }

  /**
   * The last addresses it ran, the most recent last. Fewer than asked for if it has not run
   * that many yet, and never more than the ring holds however long it has been running.
   */
  public synchronized int[] last(int many) {
    int have = (int) Math.min(steps, KEPT);
    int wanted = Math.min(many, have);
    int[] last = new int[wanted];
    for (int i = 0; i < wanted; i++) {
      last[i] = ran[(int) (steps - wanted + i & KEPT - 1)];
    }
    return last;
  }

  public synchronized void forget() {
    steps = 0;
  }

  /** Lets the machine go: from here on it runs as though this had never been watching. */
  public void close() {
    watching.off();
  }
}

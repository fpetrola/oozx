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

package com.fpetrola.oozx.speccy.modules.z80;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What watches the address bus while the processor fetches: a device that pages its ROM in when
 * the program reaches an address of the machine's ROM - 0x0008, 0x0066, 0x1708 - the way the
 * Interface 1, the +D and the Multiface do, and out again at another.
 * <p>
 * A lateral aspect of the processor, like the contention: no instruction knows of it. One bit
 * per address says whether anything is watching there, so on a machine with nothing plugged in an
 * instruction costs one test of one boolean, and on one with a device it costs one bit.
 * <p>
 * Fuse looks at some addresses before the opcode is read and at others just after; here "after"
 * is after the whole instruction, and each device says which it needs.
 */
public final class PcTraps {

  public interface Trap {
    void reached(int pc);
  }

  /** A watch on a range of addresses; {@link #off} takes it away. */
  public final class Watch {
    private final int from;
    private final int to;
    private final Trap trap;

    private Watch(int from, int to, Trap trap) {
      this.from = from;
      this.to = to;
      this.trap = trap;
    }

    public void off() {
      watches.remove(this);
      rearm();
    }
  }

  private final BitSet watched = new BitSet(0x10000);
  private final List<Watch> watches = new CopyOnWriteArrayList<>();
  private boolean armed;

  /** From now on, reaching any address from {@code from} to {@code to}, both included, tells the trap. */
  public Watch watch(int from, int to, Trap trap) {
    Watch watch = new Watch(from, to, trap);
    watches.add(watch);
    rearm();
    return watch;
  }

  public Watch watch(int address, Trap trap) {
    return watch(address, address, trap);
  }

  private void rearm() {
    watched.clear();
    for (Watch watch : watches) {
      watched.set(watch.from, watch.to + 1);
    }
    armed = !watches.isEmpty();
  }

  boolean armed() {
    return armed;
  }

  void at(int pc) {
    if (watched.get(pc)) {
      for (Watch watch : watches) {
        if (watch.from <= pc && pc <= watch.to) {
          watch.trap.reached(pc);
        }
      }
    }
  }
}

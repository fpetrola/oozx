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
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceTest extends MachineTest {

  private static final int START = 0x8000;

  /** NOP / JP 8000, so what it ran is known exactly: 8000, 8001, 8000, 8001, ... */
  private Speccy looping() {
    Speccy speccy = silentMachine();
    int[] loop = {0x00, 0xc3, 0x00, 0x80};
    for (int i = 0; i < loop.length; i++) {
      speccy.memory.poke(START + i, (byte) loop[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    return speccy;
  }

  private static void steps(Speccy speccy, int many) {
    for (int i = 0; i < many; i++) {
      speccy.cpu.step();
    }
  }

  @Test
  void itKeepsWhatRanInTheOrderItRan() {
    Speccy speccy = looping();
    Trace trace = new Trace(speccy);

    steps(speccy, 4);

    assertArrayEquals(new int[]{START, START + 1, START, START + 1}, trace.last(10),
        "two laps of the loop, the most recent last");
    assertEquals(4, trace.steps());
  }

  @Test
  void itAsksForMoreThanItHasAndGetsWhatThereIs() {
    Speccy speccy = looping();
    Trace trace = new Trace(speccy);

    steps(speccy, 2);

    assertEquals(2, trace.last(1000).length, "it has run twice, so there are two");
  }

  /** The ring is the point: a machine left running all afternoon costs the same as this. */
  @Test
  void itNeverHoldsMoreThanTheRing() {
    Speccy speccy = looping();
    Trace trace = new Trace(speccy);

    steps(speccy, Trace.KEPT + 50);

    assertEquals(Trace.KEPT, trace.last(Integer.MAX_VALUE).length, "it kept only the ring");
    assertEquals(Trace.KEPT + 50, trace.steps(), "but it knows how far the machine got");
    int[] last = trace.last(2);
    assertTrue(last[0] == START || last[0] == START + 1, "and the newest is still the newest");
  }

  @Test
  void heldItStopsTakingThingsDown() {
    Speccy speccy = looping();
    Trace trace = new Trace(speccy);
    steps(speccy, 4);

    trace.follow(false);
    steps(speccy, 20);

    assertEquals(4, trace.steps(), "it went on taking things down while held");
  }

  @Test
  void lettingGoLeavesTheMachineAsItWas() {
    Speccy speccy = looping();
    Trace trace = new Trace(speccy);
    steps(speccy, 4);

    trace.close();
    steps(speccy, 20);

    assertEquals(4, trace.steps(), "it went on counting after it let go");
  }
}

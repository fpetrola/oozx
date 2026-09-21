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
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatTest extends MachineTest {

  private static final int START = 0x8000;

  /** NOP / JP 8000: two addresses run, over and over, and nothing else in the 64K does. */
  private Speccy looping() {
    Speccy speccy = silentMachine();
    int[] loop = {0x00, 0xc3, 0x00, 0x80};
    for (int i = 0; i < loop.length; i++) {
      speccy.memory.poke(START + i, (byte) loop[i]);
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    return speccy;
  }

  @Test
  void onlyWhereTheCodeRanIsWarm() {
    Speccy speccy = looping();
    Heat heat = new Heat(speccy);

    for (int i = 0; i < 20; i++) {
      speccy.cpu.step();
    }

    assertEquals(10, heat.ever(START), "ten laps through the NOP");
    assertEquals(10, heat.ever(START + 1), "and ten through the jump");
    assertEquals(0, heat.ever(START + 2), "the jump's operand is read, never run");
    assertEquals(2, heat.placesThatRan(), "two of the 64K ran, the rest never did");
  }

  @Test
  void whatIsNotRunAnyMoreGoesCold() {
    Speccy speccy = looping();
    Heat heat = new Heat(speccy);
    for (int i = 0; i < 20; i++) {
      speccy.cpu.step();
    }
    int wasWarm = heat.lately(START);
    assertTrue(wasWarm > 0, "it was running just now");

    for (int moment = 0; moment < 200; moment++) {
      heat.fade();
    }

    assertEquals(0, heat.lately(START), "nothing ran for a long while");
    assertEquals(10, heat.ever(START), "but what it ever ran does not go away");
  }

  @Test
  void lettingGoLeavesTheMachineAsItWas() {
    Speccy speccy = looping();
    Heat heat = new Heat(speccy);
    for (int i = 0; i < 4; i++) {
      speccy.cpu.step();
    }
    long ran = heat.ever(START);

    heat.close();
    for (int i = 0; i < 20; i++) {
      speccy.cpu.step();
    }

    assertEquals(ran, heat.ever(START), "it went on counting after it let go");
  }
}

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


package com.fpetrola.oozx.speccy.tools.calls;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.Disassembly;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutinesTest extends MachineTest {

  private static final int START = 0x8000;

  private Speccy running(int[]... program) {
    Speccy speccy = silentMachine();
    for (int block = 0; block < program.length; block++) {
      for (int i = 0; i < program[block].length; i++) {
        speccy.memory.poke(START + block * 0x10 + i, (byte) program[block][i]);
      }
    }
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(START);
    speccy.cpu.getOoz80().getState().getRegister(RegisterName.SP).write(0xff00);
    return speccy;
  }

  private static void steps(Speccy speccy, int many) {
    for (int i = 0; i < many; i++) {
      speccy.cpu.step();
    }
  }

  @Test
  void aRoutineGoesFromWhereItWasCalledToItsReturn() {
    // 8000: CALL 8010 / JP 8003    8010: LD A,1 / NOP / RET
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0xc3, 0x03, 0x80},
        new int[]{0x3e, 0x01, 0x00, 0xc9});
    CallTree tree = new CallTree(speccy);
    steps(speccy, 6);

    List<Routines.Routine> found =
        Routines.found(tree.program(), new Disassembly(speccy));

    assertEquals(1, found.size());
    Routines.Routine routine = found.get(0);
    assertEquals(START + 0x10, routine.address());
    assertEquals(4, routine.bytes(), "LD A,1 is two bytes, then the NOP and the RET");
    assertEquals(3, routine.instructions());
    assertEquals("RET", routine.ends());
    assertEquals(1, routine.times());
    assertEquals(1, routine.callers(), "one place called it");
  }

  /**
   * A conditional return does not end a routine: it goes on for whoever did not take it. Getting
   * this wrong makes every routine look as short as its first early exit.
   */
  @Test
  void aConditionalReturnDoesNotEndIt() {
    // 8000: CALL 8010 / JP 8003    8010: RET Z / NOP / RET
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0xc3, 0x03, 0x80},
        new int[]{0xc8, 0x00, 0xc9});
    CallTree tree = new CallTree(speccy);
    steps(speccy, 6);

    Routines.Routine routine =
        Routines.found(tree.program(), new Disassembly(speccy)).get(0);

    assertEquals(3, routine.bytes(), "it goes on past the RET Z to the RET");
    assertEquals("RET", routine.ends());
  }

  @Test
  void theSameRoutineCalledFromTwoPlacesIsOneRoutine() {
    // 8000: CALL 8010 / CALL 8010 / JP 8006    8010: RET
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0xcd, 0x10, 0x80, 0xc3, 0x06, 0x80},
        new int[]{0xc9});
    CallTree tree = new CallTree(speccy);
    steps(speccy, 8);

    List<Routines.Routine> found =
        Routines.found(tree.program(), new Disassembly(speccy));

    assertEquals(1, found.size(), "one routine, not one per call site");
    assertEquals(2, found.get(0).times());
    assertEquals(2, found.get(0).callers(), "called from two places");
  }

  @Test
  void whatWasNeverCalledIsNotARoutine() {
    Speccy speccy = running(new int[]{0x00, 0xc3, 0x00, 0x80});
    CallTree tree = new CallTree(speccy);
    steps(speccy, 6);

    assertTrue(Routines.found(tree.program(), new Disassembly(speccy)).isEmpty(),
        "nothing was called, so nothing is a routine");
  }
}

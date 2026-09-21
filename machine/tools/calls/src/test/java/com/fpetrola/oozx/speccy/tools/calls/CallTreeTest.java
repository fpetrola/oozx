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
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallTreeTest extends MachineTest {

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
  void aCallInsideACallHangsUnderIt() {
    // 8000: CALL 8010 / JP 8003    8010: CALL 8020 / RET    8020: RET
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0xc3, 0x03, 0x80},
        new int[]{0xcd, 0x20, 0x80, 0xc9}, new int[]{0xc9});
    CallTree tree = new CallTree(speccy);

    steps(speccy, 8);

    List<CallTree.Call> program = tree.program();
    assertEquals(1, program.size(), "only one call was made from the program itself");
    CallTree.Call outer = program.get(0);
    assertEquals(START + 0x10, outer.address());
    assertEquals(Map.of(START + 3, 1), outer.back(), "it comes back to after the call");
    assertEquals(1, outer.made().size(), "the routine called one of its own");
    CallTree.Call inner = outer.made().get(0);
    assertEquals(START + 0x20, inner.address());
    assertEquals(Map.of(START + 0x13, 1), inner.back());
  }

  @Test
  void aRoutineCalledAgainAfterItReturnedIsNotInsideItself() {
    // 8000: CALL 8010 / NOP / JP 8000    8010: RET
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0x00, 0xc3, 0x00, 0x80},
        new int[]{0xc9});
    CallTree tree = new CallTree(speccy);

    steps(speccy, 12);

    assertEquals(1, tree.program().size(), "the three laps called the same routine");
    assertEquals(3, tree.program().get(0).times());
    assertTrue(tree.program().get(0).made().isEmpty(), "it called nothing of its own");
  }

  @Test
  void aConditionalCallThatDidNotGoIsNotACall() {
    // 8000: XOR A / CALL NZ,8010 / JP 8004    8010: RET
    Speccy speccy = running(new int[]{0xaf, 0xc4, 0x10, 0x80, 0xc3, 0x04, 0x80},
        new int[]{0xc9});
    CallTree tree = new CallTree(speccy);

    steps(speccy, 8);

    assertTrue(tree.program().isEmpty(), "the zero flag was set, so it never went");
  }

  @Test
  void lettingGoLeavesTheMachineAsItWas() {
    Speccy speccy = running(new int[]{0xcd, 0x10, 0x80, 0xc3, 0x03, 0x80}, new int[]{0xc9});
    CallTree tree = new CallTree(speccy);
    steps(speccy, 4);
    long counted = tree.counted();

    tree.close();
    steps(speccy, 8);

    assertEquals(counted, tree.counted(), "it went on counting after it let go");
  }
}

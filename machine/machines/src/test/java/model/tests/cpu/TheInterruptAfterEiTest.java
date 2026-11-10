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

package model.tests.cpu;

import com.fpetrola.oozx.Speccy;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EI lets interrupts in only after the instruction that follows it. That is what makes the usual
 * end of a handler, EI then RET, return before the next interrupt lands on top of it: with the
 * line still down, an interrupt taken straight after the EI would push the RET's own address and
 * nest one handler inside the other.
 */
class TheInterruptAfterEiTest extends MachineTest {

  private static final int EI = 0xFB;
  private static final int NOP = 0x00;
  private static final int AT = 0x8000;

  @Test
  void anInterruptRaisedRightAfterEiWaitsForTheNextInstruction() {
    Speccy speccy = silentMachine();
    speccy.memory.poke(AT, (byte) EI);
    speccy.memory.poke(AT + 1, (byte) NOP);
    speccy.memory.poke(AT + 2, (byte) NOP);
    var state = speccy.cpu.getOoz80().getState();
    state.getPc().write(AT);
    state.setIff1(false);
    state.setIff2(false);
    speccy.zxClock.setTStates(0);
    speccy.scheduler.clear();

    speccy.cpu.step();
    speccy.cpu.interrupt(1000);
    assertEquals(AT + 1, state.getPc().read(), "interrupted straight after EI, before the instruction that follows it");

    speccy.cpu.step();
    speccy.scheduler.runDue();
    assertEquals(0x0038, state.getPc().read(), "the instruction after EI ran and the interrupt, still pending, was not taken");
    int sp = state.getRegisterSP().read();
    assertEquals(AT + 2, speccy.memory.peek(sp) & 0xff | (speccy.memory.peek(sp + 1) & 0xff) << 8,
        "the interrupt did not return to the instruction after the one EI let through");
  }
}

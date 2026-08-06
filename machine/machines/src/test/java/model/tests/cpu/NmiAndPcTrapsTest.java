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

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two things a device on the edge connector can do to the processor: pull /NMI, and watch the bus. */
class NmiAndPcTrapsTest extends MachineTest {

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  @Test
  void anNmiPushesThePcAndGoesTo0x66WithInterruptsOff() {
    Speccy speccy = speccy();
    runFrames(speccy, 2);
    var state = speccy.cpu.getOoz80().getState();
    int pc = state.getPc().read();
    int sp = state.getRegisterSP().read();
    long tstates = speccy.zxClock.getTStates();

    speccy.cpu.nmi();
    step(speccy);

    assertEquals(0x0066, state.getPc().read(), "the processor did not go to the NMI vector");
    assertEquals(sp - 2, state.getRegisterSP().read(), "the return address was not pushed");
    assertEquals(pc, speccy.memory.peek(sp - 2) & 0xff | (speccy.memory.peek(sp - 1) & 0xff) << 8);
    assertFalse(state.isIff1(), "an NMI disables maskable interrupts");
    assertEquals(11, speccy.zxClock.getTStates() - tstates, "an NMI takes eleven T-states");
  }

  @Test
  void aWatchedAddressIsToldBeforeTheFetchAndAfterTheInstruction() {
    Speccy speccy = speccy();
    List<Integer> before = new ArrayList<>();
    List<Integer> after = new ArrayList<>();
    // 0x0038 is where the 48K ROM answers every frame's interrupt, so it is reached fifty times a
    // second - once the ROM has checked its memory and enabled them, a couple of seconds in.
    PcTraps.Watch first = speccy.cpu.beforeFetch().watch(0x0038, before::add);
    PcTraps.Watch second = speccy.cpu.afterInstruction().watch(0x0038, after::add);
    runFrames(speccy, 250);
    assertTrue(before.size() >= 1, "the interrupt routine was never seen");
    assertEquals(before.size(), after.size(), "every fetch seen before should be seen after");
    assertEquals(0x0038, before.get(0));

    first.off();
    second.off();
    int seen = before.size();
    runFrames(speccy, 4);
    assertEquals(seen, before.size(), "a watch that was taken off went on watching");
  }
}

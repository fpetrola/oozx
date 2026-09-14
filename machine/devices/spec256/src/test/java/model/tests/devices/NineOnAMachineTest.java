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


package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.z80.Processors;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A whole machine on the nine, which is where the things a machine has - a clock - can be asked. */
class NineOnAMachineTest extends MachineTest {
  private static final int INSTRUCTIONS = 1000;

  private Speccy startedOn(String core) {
    Processors.startsOn = core;
    try {
      Speccy speccy = silentMachine();
      select(speccy, speccy.machine.model(Spec48.class));
      return speccy;
    } finally {
      Processors.startsOn = null;
    }
  }

  private long timeTakenBy(String core) {
    Speccy speccy = startedOn(core);
    assertEquals(core, speccy.processors.current(), "the machine is on the processor it was asked for");
    runFrames(speccy, 2);
    long before = speccy.zxClock.getTStates();
    for (int instruction = 0; instruction < INSTRUCTIONS; instruction++) speccy.cpu.step();
    return speccy.zxClock.getTStates() - before;
  }

  @Test
  void theTimeOfAnInstructionIsTheMachinesOwnHoweverManyProcessorsFollowIt() {
    long alone = timeTakenBy("OOP");
    long inNine = timeTakenBy("Spec256");

    assertTrue(alone > INSTRUCTIONS * 4L, "a thousand instructions took at least four T-states each");
    assertEquals(alone, inNine, "eight followers count nobody's time, and the machine's clock never hears them");
  }

  @Test
  void aMachineOnTheNineBootsTheSameAsOnItsOwn() {
    Speccy alone = startedOn("OOP");
    Speccy inNine = startedOn("Spec256");
    runFrames(alone, 200);
    runFrames(inNine, 200);

    assertEquals(alone.cpu.getOoz80().getState().getRegister(com.fpetrola.z80.registers.RegisterName.PC).read(),
        inNine.cpu.getOoz80().getState().getRegister(com.fpetrola.z80.registers.RegisterName.PC).read(),
        "the machine reached the same place in its ROM");
    assertTrue(inNine.processors.all().stream().anyMatch(core -> core.name().equals("Spec256")),
        "and it is one of the processors a machine can be moved onto");
  }
}

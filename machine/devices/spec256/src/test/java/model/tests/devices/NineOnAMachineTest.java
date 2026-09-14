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

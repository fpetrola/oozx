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
package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.cpu.State;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fpetrola.z80.registers.RegisterName.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The machine on a generated core is the machine on the OOP core: same memory, registers and clock
 * after the same frames of the ROM. With this module on the classpath a machine gets the generated
 * core unless told otherwise, so it is the OOP side that is bound by hand.
 */
class GeneratedMachineCoreTest {
  @Test
  void bootsAsTheOopCoreDoes() {
    assertSameMachine(MachineTest.silentMachine(EmulatorModule.core(OopCore.class)), MachineTest.silentMachine());
  }

  /** Everything the processor was carries across, and nothing runs while it is exchanged. */
  @Test
  void changingProcessorLeavesTheMachineExactlyWhereItWas() {
    Speccy speccy = MachineTest.silentMachine();
    MachineTest.runFrames(speccy, 150);

    String before = registers(speccy);
    int tstates = speccy.zxClock.getTStates();
    String was = speccy.processors.current();

    speccy.processors.use("OOP");
    speccy.loop.applyWhatWasDeferred();

    assertEquals("OOP", speccy.processors.current(), "the machine is on the processor it was asked for");
    assertTrue(speccy.processors.all().size() > 1, "this build offers more than one processor: " + speccy.processors.all());
    assertEquals("Generated", was, "it started on the one this build prefers");
    assertEquals(before, registers(speccy), "the registers came across");
    assertEquals(tstates, speccy.zxClock.getTStates(), "the clock is the machine's, not the processor's");
  }

  /** And it goes on running as the machine it was: half the boot on one processor, half on the other. */
  @Test
  void aMachineThatChangedProcessorRunsOnAsTheOtherOneWould() {
    Speccy switching = MachineTest.silentMachine();
    MachineTest.runFrames(switching, 150);
    switching.processors.use("OOP");
    MachineTest.runFrames(switching, 150);

    Speccy oop = MachineTest.silentMachine(EmulatorModule.core(OopCore.class));
    MachineTest.runFrames(oop, 300);

    assertEquals("OOP", switching.processors.current());
    assertEquals(registers(oop), registers(switching));
    assertEquals(oop.zxClock.getTStates(), switching.zxClock.getTStates());
    assertTrue(sameRam(oop, switching), "the RAM differs");
  }

  private static boolean sameRam(Speccy one, Speccy other) {
    for (int bank = 0; bank < SpectrumMemory.SPECTRUM_RAM_PAGES; bank++) {
      if (!Arrays.equals(one.banks.ram(bank).bytes, other.banks.ram(bank).bytes)) return false;
    }
    return true;
  }

  private static void assertSameMachine(Speccy oop, Speccy generated) {
    MachineTest.runFrames(oop, 300);
    MachineTest.runFrames(generated, 300);
    assertEquals(registers(oop), registers(generated));
    assertEquals(oop.zxClock.getTStates(), generated.zxClock.getTStates());
    assertTrue(sameRam(oop, generated), "the RAM differs");
  }

  private static String registers(Speccy speccy) {
    State state = speccy.cpu.getOoz80().getState();
    return Stream.of(AF, BC, DE, HL, AFx, BCx, DEx, HLx, IX, IY, SP, PC, I, R, MEMPTR)
        .map(r -> r + "=" + state.getRegister(r).read()).collect(Collectors.joining(" "));
  }
}

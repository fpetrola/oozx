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

package model.tests.machine;

import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.EmulatorControl;
import com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The application offers a choice of processor and moves the machine onto the one it is given.
 * Exchanging one for the other is the Z80's and is proved there; what this proves is that the
 * build the desktop runs has both on its classpath and reaches them through the same seam its
 * settings window uses, so the choice is really there to make.
 */
class ProcessorChoiceTest extends MachineTest {
  @Test
  void theSettingsSeamOffersBothProcessorsAndMovesTheMachine() {
    Processors.startsOn = null;
    Speccy speccy = silentMachine();
    EmulatorControl control = new SpeccyEmulatorCore(speccy);

    assertEquals(List.of("Generated", "OOP"), control.getProcessors(), "both, in a stable order");
    assertEquals("Generated", control.getProcessor(), "a machine starts on the one this build prefers");

    control.setProcessor("OOP");
    speccy.loop.applyWhatWasDeferred();
    assertEquals("OOP", control.getProcessor());

    control.setProcessor("Generated");
    speccy.loop.applyWhatWasDeferred();
    assertEquals("Generated", control.getProcessor(), "and back");
  }
}

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
 * build the desktop runs has them on its classpath and reaches them through the same seam its
 * settings window uses, so the choice is really there to make.
 */
class ProcessorChoiceTest extends MachineTest {
  @Test
  void theSettingsSeamOffersEveryProcessorAndMovesTheMachine() {
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", null);
    Speccy speccy = silentMachine();
    EmulatorControl control = new SpeccyEmulatorCore(speccy);

    assertEquals(List.of("Generated", "OOP", "Spec256"), control.getProcessors(), "all of them, in a stable order");
    assertEquals("Generated", control.getProcessor(), "a machine starts on the one this build prefers");

    control.setProcessor("OOP");
    speccy.loop.applyWhatWasDeferred();
    assertEquals("OOP", control.getProcessor());

    control.setProcessor("Generated");
    speccy.loop.applyWhatWasDeferred();
    assertEquals("Generated", control.getProcessor(), "and back");
  }
}

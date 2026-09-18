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
package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.Speccy;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/** The core is a class of this module, written and compiled by the build. */
class GeneratedCoresTest {

  @Test
  void theJarCarriesTheCoreTheMachinesRun() {
    assertEquals("com.fpetrola.oozx.generated.GeneratedSpectrumZ80", GeneratedCores.loaded().orElseThrow().getName());
  }

  /**
   * A processor chosen in the settings must not reach the machine the generator reads. It did:
   * the model was started on the core it is the model for, which came back here to make it, and
   * the emulator went round until the heap was gone.
   */
  @Test
  void theMachineTheGeneratorReadsIsNotStartedOnWhatWasChosen() {
    com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", GeneratedMachineCore.NAME);
    try {
      Speccy model = GeneratedCores.model();
      assertEquals("OOP", model.processors.current(), "the model runs on the model's core");
      model.end();
      assertEquals(GeneratedMachineCore.NAME,
          com.fpetrola.oozx.config.Configuration.shared().valueOf("machine", "processor", String.class),
          "and the choice is left for the machine that asked");
    } finally {
      com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "processor", null);
    }
  }
}

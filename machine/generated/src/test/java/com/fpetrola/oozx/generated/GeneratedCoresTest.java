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

import com.fpetrola.oozx.speccy.modules.z80.Processors;
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
    Processors.startsOn = GeneratedMachineCore.NAME;
    try {
      Speccy model = GeneratedCores.model();
      assertEquals("OOP", model.processors.current(), "the model runs on the model's core");
      model.end();
      assertEquals(GeneratedMachineCore.NAME, Processors.startsOn, "and the choice is left for the machine that asked");
    } finally {
      Processors.startsOn = null;
    }
  }
}

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

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** The variants differ from the machine they are a variant of in exactly what they say they do. */
class VariantsTest {
  private final Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

  private Set<?> capabilitiesOf(Class<? extends SpectrumMachine> model) {
    return injector.getInstance(model).onBoard();
  }

  @Test
  void theNtscFortyEightIsAFortyEight() {
    assertEquals(Set.of(), capabilitiesOf(Spec48Ntsc.class));
    assertFalse(injector.getInstance(Spec48Ntsc.class).pagesThrough7ffd());
  }

  @Test
  void thePlusThreeEIsAPlusThree() {
    assertEquals(capabilitiesOf(SpecPlus3.class), capabilitiesOf(SpecPlus3E.class));
  }
}

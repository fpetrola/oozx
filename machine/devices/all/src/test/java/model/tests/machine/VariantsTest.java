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

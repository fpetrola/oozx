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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.peripherals.Periph;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a machine comes with, and what somebody chose to plug into it.
 * <p>
 * A machine says a peripheral is possible; whether it is actually there is a separate answer, and
 * for everything optional it was always no. The branch was written and every peripheral answered
 * the question with a flat refusal, so a Kempston joystick could be declared possible on every
 * machine and never once respond on a port.
 */
class OptionalPeripheralsTest {

  private Speccy machineWith(boolean kempstonWanted) {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.joyKempston = kempstonWanted;
    speccy.machine.selectDefault();
    return speccy;
  }

  @Test
  void whatTheMachineComesWithIsThere() {
    Speccy speccy = machineWith(false);
    assertTrue(speccy.periph.isActive(Periph.Type.ULA), "every machine has a ULA");
  }

  @Test
  void aJoystickIsThereWhenSomebodyAskedForOne() {
    assertTrue(machineWith(true).periph.isActive(Periph.Type.KEMPSTON),
        "a Kempston was asked for and did not arrive");
  }

  @Test
  void andIsNotWhenNobodyDid() {
    assertFalse(machineWith(false).periph.isActive(Periph.Type.KEMPSTON),
        "a Kempston nobody asked for is answering on its ports");
  }
}

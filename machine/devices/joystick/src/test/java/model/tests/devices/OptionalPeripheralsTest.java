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
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonStrictPeripheral;

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
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    Input.of(speccy).setup.kempstonJoystick = kempstonWanted;
    speccy.machine.selectDefault();
    return speccy;
  }

  @Test
  void aJoystickIsThereWhenSomebodyAskedForOne() {
    assertTrue(machineWith(true).peripheralRegistry.isActive(KempstonStrictPeripheral.class),
        "a Kempston was asked for and did not arrive");
  }

  @Test
  void andIsNotWhenNobodyDid() {
    assertFalse(machineWith(false).peripheralRegistry.isActive(KempstonStrictPeripheral.class),
        "a Kempston nobody asked for is answering on its ports");
  }
}

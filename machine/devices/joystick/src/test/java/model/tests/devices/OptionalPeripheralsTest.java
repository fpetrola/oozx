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

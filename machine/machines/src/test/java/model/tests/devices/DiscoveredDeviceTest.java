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
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import model.tests.devices.outside.PretendInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms peripherals are discovered purely via ServiceLoader on the classpath: PretendInterface
 * lives only in test sources, referenced by no main code or module list, yet must still be found
 * and behave as a full device - proving a new peripheral needs only a jar, not code changes.
 */
class DiscoveredDeviceTest {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  @Test
  void aDeviceNobodyCompiledInIsFoundAndRegistered() {
    Speccy speccy = speccy();
    assertNotNull(speccy.peripheralRegistry.find(PretendInterface.class),
        "the device on the classpath was never registered");
  }

  /** It said it is a 48K box, and that alone decides where it is switched on. */
  @Test
  void itIsSwitchedOnForTheMachineItSaidItFits() {
    Speccy speccy = speccy();

    speccy.machine.select(speccy.machine.model(Spec48.class));
    assertTrue(speccy.peripheralRegistry.isActive(PretendInterface.class), "not switched on for a 48K");
    assertSame(speccy.machine.model(Spec48.class), ((PretendInterface) speccy.peripheralRegistry.find(PretendInterface.class)).switchedOnFor(),
        "switched on without being told which machine for");

    speccy.machine.select(speccy.machine.model(Spec128.class));
    assertFalse(speccy.peripheralRegistry.isActive(PretendInterface.class), "still on where it does not fit");
  }

  /** And the whole point: its port answers. */
  @Test
  void itAnswersItsPort() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Spec48.class));

    assertEquals(PretendInterface.ANSWER, speccy.ports.read(PretendInterface.PORT),
        "the port of a discovered device did not answer");

    speccy.machine.select(speccy.machine.model(Spec128.class));
    assertEquals((byte) 0xff, speccy.ports.read(PretendInterface.PORT),
        "it went on answering on a machine it does not fit");
  }
}

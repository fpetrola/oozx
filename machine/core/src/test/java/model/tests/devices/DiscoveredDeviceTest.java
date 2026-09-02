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
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import model.tests.devices.outside.PretendInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A device the emulator was never built with, doing everything a device does.
 * <p>
 * PretendInterface lives in the test sources and is announced in the tests' own service file. No
 * main source mentions it, no module here installs it, and no list contains it - it is found
 * because it is on the classpath and says what it is. If this passes, adding a peripheral is
 * adding a jar.
 */
class DiscoveredDeviceTest {

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  @Test
  void aDeviceNobodyCompiledInIsFoundAndRegistered() {
    Speccy speccy = speccy();
    assertNotNull(speccy.peripherals.find(PretendInterface.class),
        "the device on the classpath was never registered");
  }

  /** It said it is a 48K box, and that alone decides where it is switched on. */
  @Test
  void itIsSwitchedOnForTheMachineItSaidItFits() {
    Speccy speccy = speccy();

    speccy.machine.select(speccy.spec48);
    assertTrue(speccy.peripherals.isActive(PretendInterface.class), "not switched on for a 48K");
    assertSame(speccy.spec48, ((PretendInterface) speccy.peripherals.find(PretendInterface.class)).switchedOnFor(),
        "switched on without being told which machine for");

    speccy.machine.select(speccy.spec128);
    assertFalse(speccy.peripherals.isActive(PretendInterface.class), "still on where it does not fit");
  }

  /** And the whole point: its port answers. */
  @Test
  void itAnswersItsPort() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.spec48);

    assertEquals(PretendInterface.ANSWER, speccy.peripherals.readPort(PretendInterface.PORT),
        "the port of a discovered device did not answer");

    speccy.machine.select(speccy.spec128);
    assertEquals((byte) 0xff, speccy.peripherals.readPort(PretendInterface.PORT),
        "it went on answering on a machine it does not fit");
  }
}

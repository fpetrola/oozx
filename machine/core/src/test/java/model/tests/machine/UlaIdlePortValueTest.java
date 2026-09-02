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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bit 6 of the ULA's own port, which no key drives, is not the same on every machine: an issue 3
 * 48K and a 128K follow the last write to bit 4, an issue 2 follows bits 3 and 4 together, and the
 * +3 family holds it low always. Software reads it to tell the machines apart, so each answer is
 * the machine's to give - the ULA only stores what it is told.
 */
class UlaIdlePortValueTest {

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  private void idleIs(int expected, SpectrumMachine machine, int lastOut) {
    assertEquals((byte) expected, machine.ulaPortIdleValue((byte) lastOut),
        machine.getName() + " after writing " + Integer.toHexString(lastOut));
  }

  @Test
  void eachMachineAnswersForItsOwnPort() {
    Speccy speccy = speccy();
    speccy.settings.current.issue2 = false;

    idleIs(0xff, speccy.spec48, 0x10);
    idleIs(0xbf, speccy.spec48, 0x08);
    idleIs(0xff, speccy.spec128, 0x10);
    idleIs(0xbf, speccy.spec128, 0x08);
    idleIs(0xff, speccy.pentagon, 0x10);

    idleIs(0xbf, speccy.specPlus3, 0x10);
    idleIs(0xbf, speccy.specPlus2a, 0x10);
    idleIs(0xbf, speccy.specPlus3e, 0x10);
  }

  /** Only the 48K reads bit 3 as well, and only when the setting says the machine is an issue 2. */
  @Test
  void issue2IsThe48KsBusiness() {
    Speccy speccy = speccy();
    speccy.settings.current.issue2 = true;

    idleIs(0xff, speccy.spec48, 0x08);
    idleIs(0xff, speccy.spec48Ntsc, 0x08);
    idleIs(0xbf, speccy.spec128, 0x08);
  }

  /** And that the ULA asks: writing to its port leaves the machine's answer where reads find it. */
  @Test
  void theUlaStoresWhatTheMachineAnswers() {
    Speccy speccy = speccy();
    speccy.settings.current.issue2 = false;
    speccy.machine.select(speccy.specPlus3);

    speccy.ula.write(0xfe, (byte) 0x10);
    assertEquals((byte) 0xbf, (byte) (speccy.ula.read(0xfefe, new byte[1]) | 0x1f),
        "a +3 holding bit 6 low is what the ULA should have kept");

    speccy.machine.select(speccy.spec128);
    speccy.ula.write(0xfe, (byte) 0x10);
    assertEquals((byte) 0xff, (byte) (speccy.ula.read(0xfefe, new byte[1]) | 0x1f),
        "a 128K follows bit 4, and the ULA kept the +3's answer");
  }
}

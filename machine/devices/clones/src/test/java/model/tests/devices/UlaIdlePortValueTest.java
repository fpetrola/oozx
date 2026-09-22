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
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.devices.ula.UlaPeripheral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Undriven bit 6 of the ULA port differs per model, and software uses it to identify the
 * machine: issue-3 48K/128K mirror the last write to bit 4, issue-2 mirrors bits 3-4 together,
 * +3-family always reads low. The ULA itself just stores whatever it is told.
 */
class UlaIdlePortValueTest {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  private void idleIs(int expected, SpectrumMachine machine, int lastOut) {
    assertEquals((byte) expected, machine.ulaPortIdleValue((byte) lastOut),
        machine.getName() + " after writing " + Integer.toHexString(lastOut));
  }

  @Test
  void eachMachineAnswersForItsOwnPort() {
    Speccy speccy = speccy();
    speccy.machine.unit.issue2 = false;

    idleIs(0xff, speccy.machine.model(Spec48.class), 0x10);
    idleIs(0xbf, speccy.machine.model(Spec48.class), 0x08);
    idleIs(0xff, speccy.machine.model(Spec128.class), 0x10);
    idleIs(0xbf, speccy.machine.model(Spec128.class), 0x08);
    idleIs(0xff, speccy.machine.model(Pentagon.class), 0x10);

    idleIs(0xbf, speccy.machine.model(SpecPlus3.class), 0x10);
    idleIs(0xbf, speccy.machine.model(SpecPlus2A.class), 0x10);
    idleIs(0xbf, speccy.machine.model(SpecPlus3E.class), 0x10);
  }

  /** Only the 48K reads bit 3 as well, and only when the setting says the machine is an issue 2. */
  @Test
  void issue2IsThe48KsBusiness() {
    Speccy speccy = speccy();
    speccy.machine.unit.issue2 = true;

    idleIs(0xff, speccy.machine.model(Spec48.class), 0x08);
    idleIs(0xff, speccy.machine.model(Spec48Ntsc.class), 0x08);
    idleIs(0xbf, speccy.machine.model(Spec128.class), 0x08);
  }

  /** And that the ULA asks: writing to its port leaves the machine's answer where reads find it. */
  @Test
  void theUlaStoresWhatTheMachineAnswers() {
    Speccy speccy = speccy();
    speccy.machine.unit.issue2 = false;
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));

    speccy.ports.write(0xfe, (byte) 0x10);
    assertEquals((byte) 0xbf, (byte) (speccy.ports.read(0xfefe) | 0x1f),
        "a +3 holding bit 6 low is what the ULA should have kept");

    speccy.machine.select(speccy.machine.model(Spec128.class));
    speccy.ports.write(0xfe, (byte) 0x10);
    assertEquals((byte) 0xff, (byte) (speccy.ports.read(0xfefe) | 0x1f),
        "a 128K follows bit 4, and the ULA kept the +3's answer");
  }

  @Test
  void everyMachineComesWithOne() {
    Speccy speccy = speccy();
    speccy.machine.selectDefault();
    assertTrue(speccy.peripheralRegistry.isActive(UlaPeripheral.class), "every machine has a ULA");
  }

  @Test
  void twoEmulatorsDoNotShareOne() {
    assertNotSame(speccy().peripheralRegistry.find(UlaPeripheral.class), speccy().peripheralRegistry.find(UlaPeripheral.class),
        "two emulators were handed the same ULA device, which holds the machine it was switched on for");
  }
}

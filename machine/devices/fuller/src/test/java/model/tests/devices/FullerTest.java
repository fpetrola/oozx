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
import com.fpetrola.oozx.speccy.devices.fuller.FullerPeripheral;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullerTest {

  private static class Loudest extends SilentSoundDevice {
    int peak;

    public void sound_lowlevel_frame(int[] data, int length) {
      for (int i = 0; i < length; i++) {
        peak = Math.max(peak, Math.abs(data[i]));
      }
    }
  }

  private Speccy aFortyEightWithAFuller() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).toInstance(new Loudest()));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.settings.current.sound = true;
    speccy.machine.select(speccy.spec48);
    ((FullerPeripheral) speccy.peripherals.find(FullerPeripheral.class)).plugIn(true);
    speccy.peripherals.update();
    return speccy;
  }

  @Test
  void aFortyEightWithOnePlaysMusicWrittenToItsPorts() {
    Speccy speccy = aFortyEightWithAFuller();
    assertTrue(speccy.peripherals.isActive(FullerPeripheral.class));
    speccy.peripherals.writePort(0x3f, (byte) 0);
    speccy.peripherals.writePort(0x5f, (byte) 0x50);
    speccy.peripherals.writePort(0x3f, (byte) 8);
    speccy.peripherals.writePort(0x5f, (byte) 0x0F);
    speccy.peripherals.writePort(0x3f, (byte) 7);
    speccy.peripherals.writePort(0x5f, (byte) 0x3E);
    speccy.sound.frame();
    assertTrue(((Loudest) speccy.sound.getJavaSoundDevice()).peak > 0, "the box made no sound");
    assertEquals(0x3E, speccy.peripherals.readPort(0x3f) & 0xff, "the register port reads back the selected register's value");
  }

  @Test
  void itsJoystickReadsOnPort0x7fActiveLow() {
    Speccy speccy = aFortyEightWithAFuller();
    speccy.settings.current.joystickKeyboardOutput = Joystick.JoystickType.JOYSTICK_TYPE_FULLER;
    assertEquals(0xff, speccy.peripherals.readPort(0x7f) & 0xff, "nothing pressed reads all high");
    speccy.joystick.press(speccy.joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    assertEquals(0x7f, speccy.peripherals.readPort(0x7f) & 0xff, "fire pulls bit 7 low");
    speccy.joystick.press(speccy.joystick.JOYSTICK_KEYBOARD, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE, false);
  }

  @Test
  void itWasSoldForTheFortyEight() {
    Speccy speccy = aFortyEightWithAFuller();
    FullerPeripheral box = (FullerPeripheral) speccy.peripherals.find(FullerPeripheral.class);
    assertTrue(box.fitsOn(speccy.spec48));
    assertFalse(box.fitsOn(speccy.spec128));
  }
}

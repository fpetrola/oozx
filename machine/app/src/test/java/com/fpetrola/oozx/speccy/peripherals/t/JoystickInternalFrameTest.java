/*
 * Copyright (c) 2026 Fernando Petrola
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

package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.modules.Joystick.JoystickButton;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The window shows what the machine's Kempston port reads, and names the gamepad doing it. */
class JoystickInternalFrameTest {
  @Test
  void itShowsWhatTheMachineInFrontReadsAndWhoPressesIt() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    JInternalFrame machine = new JInternalFrame("machine");
    JoystickInternalFrame window = new JoystickInternalFrame(w -> w == machine ? speccy : null, () -> "DualSense");
    window.setMachineWindow(machine);
    speccy.joystick.press(0, JoystickButton.JOYSTICK_BUTTON_UP, true);
    speccy.joystick.press(0, JoystickButton.JOYSTICK_BUTTON_FIRE, true);

    assertEquals(JoystickInternalFrame.UP | JoystickInternalFrame.FIRE, window.pressed(), "up and fire, as port 31 reads them");
    window.setMachineWindow(null);
    assertEquals(0, window.pressed(), "with no machine there is nothing to read");
    window.setMachineWindow(machine);
    window.refresh();
    assertTrue(window.getTitle().contains("DualSense"), "the gamepad's name: " + window.getTitle());
    assertEquals("Kempston off", window.reading(), "whether the machine has the interface on");
  }
}

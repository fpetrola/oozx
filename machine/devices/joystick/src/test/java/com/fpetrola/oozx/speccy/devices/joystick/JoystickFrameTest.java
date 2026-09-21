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

package com.fpetrola.oozx.speccy.devices.joystick;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick.JoystickButton;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The window shows what the machine's Kempston port reads, and names the gamepad doing it. */
class JoystickFrameTest {

  /** A machine's window, which is how a window clipped onto it finds the machine. */
  private static JInternalFrame machineWindow(Speccy speccy) {
    class Window extends JInternalFrame implements com.fpetrola.oozx.speccy.devices.EmulatorWindow {
      public Speccy machine() {
        return speccy;
      }

      public javax.swing.JComponent picture() {
        return null;
      }
    }
    return new Window();
  }

  @Test
  void itShowsWhatTheMachineInFrontReadsAndWhoPressesIt() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    JInternalFrame machine = machineWindow(speccy);
    JoystickFrame window = new JoystickFrame(() -> "DualSense");
    window.setMachineWindow(machine);
    Input.of(speccy).joystick().press(0, JoystickButton.JOYSTICK_BUTTON_UP, true);
    Input.of(speccy).joystick().press(0, JoystickButton.JOYSTICK_BUTTON_FIRE, true);

    assertEquals(JoystickFrame.UP | JoystickFrame.FIRE, window.pressed(), "up and fire, as port 31 reads them");
    window.setMachineWindow(null);
    assertEquals(0, window.pressed(), "with no machine there is nothing to read");
    window.setMachineWindow(machine);
    window.refresh();
    assertTrue(window.getTitle().contains("DualSense"), "the gamepad's name: " + window.getTitle());
    assertEquals("Kempston off", window.reading(), "whether the machine has the interface on");
  }
}

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
package com.fpetrola.oozx.speccy.peripherals.t;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The speed slider's notches, and the drop-down a right click on a button opens. */
class SlidersUnderTheButtonsTest {

  /** A quarter of real time to sixteen times it, doubling every two notches, and back again. */
  @Test
  void theSpeedNotchesDoubleEveryTwoAndComeBack() {
    assertEquals(25, EmulatorInternalFrame.speedAt(-4));
    assertEquals(100, EmulatorInternalFrame.speedAt(0));
    assertEquals(200, EmulatorInternalFrame.speedAt(2));
    assertEquals(1600, EmulatorInternalFrame.speedAt(8));
    for (int notch = -4; notch <= 8; notch++) {
      assertEquals(notch, EmulatorInternalFrame.notchOf(EmulatorInternalFrame.speedAt(notch)), "notch " + notch);
    }
    assertEquals(8, EmulatorInternalFrame.notchOf(com.fpetrola.oozx.Settings.SettingsInfo.UNLIMITED_SPEED), "no limit sits at the top");
  }

  @Test
  void aRightClickOnTheButtonDropsTheSliderDown() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless(), "a popup needs a screen to drop down on");
    JButton button = new JButton("turbo");
    JSlider slider = new JSlider();
    JFrame frame = new JFrame();
    try {
      SwingUtilities.invokeAndWait(() -> {
        frame.add(button);
        frame.pack();
        frame.setVisible(true);
        Widgets.popUpOnRightClick(button, slider);
        long now = System.currentTimeMillis();
        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_PRESSED, now, 0, 2, 2, 1, true, MouseEvent.BUTTON3));
        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_RELEASED, now, 0, 2, 2, 1, true, MouseEvent.BUTTON3));
      });
      SwingUtilities.invokeAndWait(() -> { });
      assertTrue(slider.isShowing(), "the slider did not drop down on a right click");
      assertTrue(SwingUtilities.getAncestorOfClass(JPopupMenu.class, slider) != null, "it is not in a popup");
    } finally {
      SwingUtilities.invokeAndWait(frame::dispose);
    }
  }
}

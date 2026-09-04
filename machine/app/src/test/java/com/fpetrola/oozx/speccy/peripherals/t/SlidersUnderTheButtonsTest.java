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
import com.fpetrola.oozx.speccy.peripherals.t.Widgets;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The speed slider's two halves, and the drop-down a right click on a button opens. */
class SlidersUnderTheButtonsTest {

  /** The left half is the quarter to ten times real time, the right half the rest; each speed has its place. */
  @Test
  void theSliderHasTwoHalvesAndEachSpeedHasItsPlace() {
    assertEquals(25, EmulatorInternalFrame.speedAt(0));
    assertEquals(EmulatorInternalFrame.KNEE_SPEED, EmulatorInternalFrame.speedAt(500));
    assertEquals(EmulatorInternalFrame.TOP_SPEED, EmulatorInternalFrame.speedAt(1000));
    assertEquals(500, EmulatorInternalFrame.positionOf(EmulatorInternalFrame.KNEE_SPEED), "the knee sits in the middle");
    for (int speed : new int[]{25, 50, 100, 200, 300, 500, 1000, 5000, 10000, 20000, 30000}) {
      int back = EmulatorInternalFrame.speedAt(EmulatorInternalFrame.positionOf(speed));
      assertTrue(Math.abs(back - speed) <= (speed <= 1000 ? 2 : 60), speed + "% came back as " + back);
    }
  }

  @Test
  void aRightClickOnTheButtonOpensTheSliderOverTheBarNotUnderIt() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless(), "a popup needs a screen to open on");
    JButton button = new JButton("turbo");
    JSlider slider = new JSlider();
    JFrame frame = new JFrame();
    try {
      SwingUtilities.invokeAndWait(() -> {
        frame.add(button);
        frame.add(javax.swing.Box.createVerticalStrut(120), java.awt.BorderLayout.NORTH);
        frame.pack();
        frame.setLocation(300, 300);
        frame.setVisible(true);
      });
      // The window manager places the frame a moment after it is shown; a popup opened before
      // that is measured against where the button was, not where it ends up.
      Thread.sleep(300);
      // Looked at right after the click, on the event thread: a popup holding a slider is a
      // window of its own that takes the focus, and a frame nobody is really using lets go of
      // the popup as soon as the focus moves, which a person's frame does not.
      String[] where = new String[1];
      SwingUtilities.invokeAndWait(() -> {
        Widgets.popUpOnRightClick(button, slider);
        long now = System.currentTimeMillis();
        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_PRESSED, now, 0, 2, 2, 1, true, MouseEvent.BUTTON3));
        button.dispatchEvent(new MouseEvent(button, MouseEvent.MOUSE_RELEASED, now, 0, 2, 2, 1, true, MouseEvent.BUTTON3));
        if (!slider.isShowing()) where[0] = "the slider did not open on a right click";
        else if (SwingUtilities.getAncestorOfClass(JPopupMenu.class, slider) == null) where[0] = "it is not in a popup";
        else if (slider.getLocationOnScreen().y + slider.getHeight() > button.getLocationOnScreen().y + button.getHeight())
          where[0] = "the slider reaches below the button, over what is under the bar";
      });
      assertEquals(null, where[0]);
    } finally {
      SwingUtilities.invokeAndWait(frame::dispose);
    }
  }

  @Test
  void aClickAnywhereOnTheTrackJumpsTheThumbThere() {
    JSlider slider = new JSlider(0, 1000, 0);
    slider.setSize(100, 20);   // no border, so the track is the whole width
    Widgets.jumpToClick(slider);
    slider.dispatchEvent(new MouseEvent(slider, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 75, 10, 1, false, MouseEvent.BUTTON1));
    assertTrue(Math.abs(slider.getValue() - 750) <= 20, "a click three quarters along should land near 750, was " + slider.getValue());
    slider.dispatchEvent(new MouseEvent(slider, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 0, 10, 1, false, MouseEvent.BUTTON1));
    assertEquals(0, slider.getValue(), "a click at the far left is the minimum");
  }
}

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
package com.fpetrola.oozx.speccy.desktop;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import com.fpetrola.oozx.speccy.windows.Widgets;
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

  /**
   * Why the picture's menu asks the machine what speed it is running at instead of asking the
   * slider, which is the thing a person can see: a position is a whole number over a range of
   * forty thousand, so real time does not survive the trip and comes back one short. Anything
   * that ticked the speed by comparing against the slider would never tick real time at all.
   */
  @Test
  void theSliderCannotSayExactlyWhatSpeedItIsShowing() {
    assertEquals(99, EmulatorInternalFrame.speedAt(EmulatorInternalFrame.positionOf(100)));
  }

  @Test
  void aRightClickOnTheButtonOpensAThinUprightSliderUnderIt() throws Exception {
    assumeFalse(GraphicsEnvironment.isHeadless(), "a popup needs a screen to open on");
    JButton button = new JButton("turbo");
    JSlider slider = Widgets.upright(new JSlider(), 24, 160);
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
        else if (slider.getLocationOnScreen().y < button.getLocationOnScreen().y + button.getHeight())
          where[0] = "the slider covers the bar instead of hanging under its button";
        else if (slider.getWidth() > 40)
          where[0] = "the slider is " + slider.getWidth() + " wide: over the picture it has to be thin";
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

    JSlider upright = Widgets.upright(new JSlider(0, 1000, 0), 20, 100);
    upright.setSize(20, 100);
    upright.dispatchEvent(new MouseEvent(upright, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 10, 25, 1, false, MouseEvent.BUTTON1));
    assertTrue(Math.abs(upright.getValue() - 750) <= 20, "upright, a click a quarter of the way down is three quarters up, was " + upright.getValue());
  }
}

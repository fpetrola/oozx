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

package com.fpetrola.oozx.rzx;
import org.junit.jupiter.api.Test;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the player sits, which is arithmetic and therefore testable.
 * <p>
 * The rest of that window needs eyes - whether the compact form looks right, whether the sticky
 * distance feels sticky - but where it lands does not, and it is the part that goes wrong
 * quietly: a window that attaches a title bar too low, or follows its machine everywhere except
 * when the machine is resized, looks almost right and is not.
 */
class RzxPlayerDockingTest {

  private static RzxPlayerInternalFrame player() {
    return new RzxPlayerInternalFrame(1, one -> null, (one, session) -> { });
  }

  /**
   * Lets the moves be delivered. Moving a component POSTS its event rather than calling the
   * listener there and then, so a test on the main thread reads the old position and reports a
   * window that does not follow when in a running program it follows perfectly well.
   */
  private static void settle() {
    try {
      SwingUtilities.invokeAndWait(() -> { });
    } catch (Exception interrupted) {
      throw new IllegalStateException(interrupted);
    }
  }

  /**
   * The two borders that meet where the frames touch.
   * <p>
   * Written as the insets rather than as a number, because the number is whatever the look and
   * feel draws and the CONTRACT is that the two frames overlap by exactly the edges they both
   * draw - so the picture and the buttons touch instead of sitting a seam apart.
   */
  private static int seam(JInternalFrame machine, RzxPlayerInternalFrame player) {
    return machine.getInsets().bottom + player.getInsets().top;
  }

  private static int sideSeam(JInternalFrame machine, RzxPlayerInternalFrame player) {
    return machine.getInsets().left + player.getInsets().right;
  }

  private static JInternalFrame machine(int x, int y, int width, int height) {
    JInternalFrame frame = new JInternalFrame("machine");
    frame.setBounds(x, y, width, height);
    return frame;
  }

  @Test
  void it_sits_under_the_machine_at_the_machine_s_width() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(60, 40, 520, 380);
    player.setMachineWindow(machine);

    Rectangle at = player.getBounds();
    assertEquals(60, at.x, "not lined up with the machine's left edge");
    assertEquals(40 + 380 - seam(machine, player), at.y, "not directly under the machine");
    assertTrue(seam(machine, player) < 12,
        "the overlap should be two borders, not a margin: " + seam(machine, player));
    assertEquals(520, at.width, "not the same width as the machine");
    assertTrue(at.height > 0 && at.height < 200,
        "the compact form should be a toolbar, not a window: " + at.height);
  }

  @Test
  void it_follows_the_machine_about() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(60, 40, 520, 380);
    player.setMachineWindow(machine);

    machine.setBounds(300, 120, 640, 300);
    settle();
    assertEquals(300, player.getX(), "did not follow the machine sideways");
    assertEquals(120 + 300 - seam(machine, player), player.getY(),
        "did not follow the machine down");
    assertEquals(640, player.getWidth(), "did not take the machine's new width");
  }

  @Test
  void a_drag_that_ends_near_an_edge_attaches_to_that_edge() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    player.setMachineWindow(machine);

    // Left just past the machine's right-hand edge, overlapping it vertically.
    player.setBounds(400 + 100 + 6, 150, 300, 120);
    assertTrue(sideSeam(machine, player) < 40,
        "the reach has to cover the seam, or nothing on that side can ever attach");
    player.snapIfNear();
    assertEquals(RzxPlayerInternalFrame.Dock.RIGHT, player.dockedTo(),
        "did not attach to the side it was left against");
    assertEquals(500 - sideSeam(machine, player), player.getX(),
        "attached to the right edge but not flush with it");
    assertEquals(100, player.getY(), "attached to a side should line up with the machine's top");
    // And its size is left alone. Stretching it to the machine's height turns a toolbar into a
    // column of empty space, and a window that resizes itself because it drifted near something
    // else is a window arguing with whoever is holding it.
    assertEquals(120, player.getHeight(), "attaching to a side resized it");
    assertEquals(300, player.getWidth(), "attaching to a side resized it");
  }

  @Test
  void a_drag_that_ends_well_away_lets_go() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    player.setMachineWindow(machine);

    player.setBounds(900, 700, 300, 120);
    player.snapIfNear();
    assertEquals(RzxPlayerInternalFrame.Dock.FREE, player.dockedTo(),
        "stayed attached to a window it is nowhere near");

    // And having let go, it stays where it was put when the machine moves.
    machine.setBounds(10, 10, 400, 300);
    settle();
    assertEquals(900, player.getX(), "a detached player was dragged along by the machine");
    assertEquals(700, player.getY(), "a detached player was dragged along by the machine");
  }

  /**
   * The sequence that was reported: dragged away, dragged back, and then the machine moved.
   * <p>
   * It snapped into place and then did not follow, which means it looked attached and was not.
   * Anything that leaves those two disagreeing is worse than not snapping at all.
   */
  @Test
  void snapping_back_really_re_attaches() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    player.setMachineWindow(machine);

    player.setBounds(900, 700, 300, 120);
    player.snapIfNear();
    assertEquals(RzxPlayerInternalFrame.Dock.FREE, player.dockedTo(), "did not let go");

    // Dropped just under the machine again, a few pixels off, the way a hand leaves it.
    player.setBounds(104, 100 + 300 + 5, 300, 120);
    player.snapIfNear();
    assertEquals(RzxPlayerInternalFrame.Dock.BOTTOM, player.dockedTo(), "did not take hold again");

    // And having taken hold, looking again must not undo it. This is what went wrong in the
    // running program: placing the window put it a seam inside the edge, the next look measured
    // that seam against the reach, and it let go of itself without anybody touching it.
    player.snapIfNear();
    assertEquals(RzxPlayerInternalFrame.Dock.BOTTOM, player.dockedTo(),
        "let go of itself the second time it looked");

    machine.setBounds(250, 260, 400, 300);
    settle();
    assertEquals(250, player.getX(), "snapped into place but then did not follow");
    assertEquals(260 + 300 - seam(machine, player), player.getY(),
        "snapped into place but then did not follow");
  }

  /**
   * Attached, it belongs in front of its machine and nowhere else in the order.
   * <p>
   * Otherwise anything that brings itself to the front - the game browser does, every time it is
   * used - comes up over the controls too, and the bar attached to a window ends up buried under
   * something that has nothing to do with it.
   */
  @Test
  void attached_it_rides_directly_in_front_of_its_machine() {
    JDesktopPane desktop = new JDesktopPane();
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(60, 40, 520, 380);
    JInternalFrame browser = machine(0, 0, 300, 300);
    desktop.add(machine);
    desktop.add(player);
    desktop.add(browser);
    player.setMachineWindow(machine);

    browser.toFront();
    machine.toFront();                     // as clicking the machine does
    machine.setBounds(60, 40, 520, 380 + 1);
    settle();

    assertEquals(desktop.getComponentZOrder(machine) - 1, desktop.getComponentZOrder(player),
        "the player is not directly in front of its machine");
    assertTrue(desktop.getComponentZOrder(player) < desktop.getComponentZOrder(browser),
        "something unrelated is sitting over the controls");
  }

  @Test
  void expanding_keeps_it_attached_and_only_makes_it_taller() {
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(60, 40, 520, 380);
    player.setMachineWindow(machine);
    int compact = player.getHeight();

    player.setCompact(false);
    assertTrue(player.getHeight() > compact, "expanding did not make it taller");
    assertEquals(60, player.getX(), "expanding moved it off the machine");
    assertEquals(520, player.getWidth(), "expanding changed its width");
    assertEquals(40 + 380 - seam(machine, player), player.getY(),
        "expanding moved it off the bottom edge");
  }

  /**
   * Opening a recording while another plays gave a player nobody could see attached to anything:
   * the previous machine window was closed while the player still held it, which read as the
   * person closing the machine, so the player dropped the recording just opened and let go.
   */
  @Test
  void another_recording_moves_it_under_the_new_machine_window_and_plays() throws Exception {
    File recording = model.harness.TestFiles.testFile("/rzx/jsw-full.rzx");
    JDesktopPane desktop = new JDesktopPane();
    List<JInternalFrame> machines = new ArrayList<>();
    RzxPlayerInternalFrame player = new RzxPlayerInternalFrame(1, one -> null, (one, session) ->
        SwingUtilities.invokeLater(() -> {
          JInternalFrame machine = machine(60 + 200 * machines.size(), 40, 520, 380);
          machines.add(machine);
          desktop.add(machine);
          one.setMachineWindow(machine);
        }));
    desktop.add(player);
    SwingUtilities.invokeAndWait(() -> player.openRecording(recording));
    settle();
    SwingUtilities.invokeAndWait(() -> player.openRecording(recording));
    settle();

    assertEquals(2, machines.size(), "each recording brings its own machine");
    assertTrue(machines.get(0).isClosed(), "the previous machine's window was left behind");
    assertFalse(player.isClosed(), "the player closed itself");
    assertNotSame(machines.get(0), player.getMachineWindow(), "still holding the previous window");
    assertEquals(260, player.getX(), "not lined up with the new machine's left edge");
    assertEquals(40 + 380 - seam(machines.get(1), player), player.getY(), "not under the new machine");
    assertTrue(player.getTitle().contains("Playing"), "opened but not playing: " + player.getTitle());
    player.dispose();
  }

  @Test
  void nearingAMachineLightsBothWindowsUntilLetGo() throws Exception {
    JDesktopPane desktop = new JDesktopPane();
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    // Each step on the event thread, in the order a drag produces it, so the moves a step posts
    // are dealt with before the next step looks.
    SwingUtilities.invokeAndWait(() -> {
      desktop.add(machine);
      desktop.add(player);
      player.setMachineWindow(machine);
      // Lifted a few pixels off the bottom edge, the way a hand holds it just before dropping.
      player.setBounds(104, 100 + 300 + 5, 300, 120);
      player.previewSnap();
    });
    settle();
    assertSame(machine, player.previewTarget(), "no hint while hovering the machine it would join");
    // Carried well away: the hint goes.
    SwingUtilities.invokeAndWait(() -> {
      player.setBounds(900, 700, 300, 120);
      player.previewSnap();
    });
    settle();
    assertNull(player.previewTarget(), "still hinting when nowhere near a machine");
    // Back near it and then let go: the drop turns the hint off, attached or not.
    SwingUtilities.invokeAndWait(() -> {
      player.setBounds(104, 100 + 300 + 5, 300, 120);
      player.previewSnap();
    });
    settle();
    assertSame(machine, player.previewTarget(), "did not light up on the way back");
    SwingUtilities.invokeAndWait(player::snapIfNear);
    settle();
    assertNull(player.previewTarget(), "the hint outlived the drop");
  }

  /**
   * A hand that stops to think, still holding the button, has not decided: the window stays where
   * it is until the button is let go. It used to attach itself after a moment of stillness.
   */
  @Test
  void aDragThatPausesStaysLooseUntilTheButtonIsLetGo() throws Exception {
    JDesktopPane desktop = new JDesktopPane();
    RzxPlayerInternalFrame player = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    SwingUtilities.invokeAndWait(() -> {
      desktop.add(machine);
      desktop.add(player);
      player.setMachineWindow(machine);
      player.setBounds(900, 700, 300, 120);
      player.snapIfNear();
    });
    settle();
    assertEquals(RzxPlayerInternalFrame.Dock.FREE, player.dockedTo(), "let go far away, it should be loose");
    // Dragged back within reach and held there.
    SwingUtilities.invokeAndWait(() -> player.setBounds(104, 100 + 300 + 5, 300, 120));
    settle();
    Thread.sleep(300);
    settle();
    assertEquals(RzxPlayerInternalFrame.Dock.FREE, player.dockedTo(), "attached itself while the button was still down");
    SwingUtilities.invokeAndWait(() -> Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
        new MouseEvent(player, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1)));
    settle();
    settle();
    assertEquals(RzxPlayerInternalFrame.Dock.BOTTOM, player.dockedTo(), "letting go did not attach it");
  }

  /**
   * Two windows on one edge take half of it each and stay there. Each read the gap the other left
   * and stepped past it, over and over, until both had walked off the edge and one was left with
   * no width at all.
   */
  @Test
  void twoWindowsOnOneEdgeShareItAndStayPut() throws Exception {
    JDesktopPane desktop = new JDesktopPane();
    RzxPlayerInternalFrame first = player();
    RzxPlayerInternalFrame second = player();
    JInternalFrame machine = machine(100, 100, 400, 300);
    SwingUtilities.invokeAndWait(() -> {
      desktop.add(machine);
      desktop.add(first);
      desktop.add(second);
      first.setMachineWindow(machine);
      second.setMachineWindow(machine);
    });
    settle();

    assertEquals(100, first.getX(), "the first does not start at the machine's left edge");
    assertEquals(200, first.getWidth(), "the first did not take half the edge");
    assertEquals(300, second.getX(), "the second does not start where the first ends");
    assertEquals(200, second.getWidth(), "the second did not take the other half");
    assertEquals(first.getY(), second.getY(), "both sit on the same edge");

    Rectangle firstAt = first.getBounds();
    Rectangle secondAt = second.getBounds();
    for (int again = 0; again < 5; again++) {
      SwingUtilities.invokeAndWait(() -> {
        first.setMachineWindow(machine);
        second.setMachineWindow(machine);
      });
      settle();
    }
    assertEquals(firstAt, first.getBounds(), "the first walked off after being placed again");
    assertEquals(secondAt, second.getBounds(), "the second walked off after being placed again");

    // And when one is carried away, the other takes the edge back.
    SwingUtilities.invokeAndWait(() -> {
      second.setBounds(900, 700, 300, 120);
      second.snapIfNear();
    });
    settle();
    assertEquals(RzxPlayerInternalFrame.Dock.FREE, second.dockedTo(), "it did not let go");
    assertEquals(100, first.getX(), "the one left behind moved");
    assertEquals(400, first.getWidth(), "the one left behind did not take the whole edge back");
  }
}

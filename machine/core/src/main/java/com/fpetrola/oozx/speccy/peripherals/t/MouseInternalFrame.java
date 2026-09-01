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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMouse;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMouse.Watcher;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMousePeripheral;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.function.Function;

/**
 * A Kempston Mouse, plugged into the machine it is clipped to.
 * <p>
 * The mouse itself is the one on the desk: moving it over the machine's picture is moving it,
 * and its buttons are that machine's buttons. What this window adds is somewhere to see what the
 * machine is being told, because a mouse is invisible until a program draws a pointer with it,
 * and a program that draws no pointer looks exactly like a mouse that is not working.
 */
public class MouseInternalFrame extends AttachedFrame {

  private static final int REFRESH_MILLIS = 50;

  /** The screen the hand is really moving across, whatever size the window showing it is. */

  private final Function<JInternalFrame, Speccy> machineOf;
  private final Counters counters = new Counters();
  private final JLabel reading = new JLabel();

  /**
   * How many counts the machine is told about for each pixel the hand moves, times a hundred.
   * <p>
   * There is no right number to hard-code. A program decides for itself how far its pointer
   * goes per count - most halve it - and the picture on screen is drawn at whatever size the
   * window happens to be, so the same hand movement is a different number of pixels from one
   * moment to the next. Rather than guess, this is a knob: turn it while watching the program
   * being pointed at, which is the only place the answer can be seen.
   * <p>
   * Seventy to begin with, and not a hundred, from a measurement: a hand at an ordinary speed
   * was putting 175 counts between two readings of a program polling fifty times a second, and
   * a reading can only carry 127. Seventy per cent of 175 is 122, which fits. Record, in this
   * window, is how to find the number for another hand or another program.
   */
  private final JSlider sensitivity = new JSlider(25, 400, 70);

  /**
   * Whether the pointer is held inside the machine's picture, which is what gives it no edges.
   * <p>
   * Ported from Fuse, which grabs the pointer and warps it back to the middle after every
   * movement - see ui_mouse_motion and SDL_WarpMouse in ui/sdl/sdlui.c. Without it the hand runs
   * out of window: the range of the mouse is the width of the picture and no more, and every
   * stroke that leaves it is lost. With it there is nowhere to run out to.
   */
  private final JToggleButton hold = new JToggleButton("Hold");

  /** Writes down both ends of the wire at once, so they can be laid side by side afterwards. */
  private final JToggleButton record = new JToggleButton("Record");

  private final Recording recording = new Recording();

  /**
   * What was left over from the last move, kept rather than thrown away.
   * <p>
   * Below one to one, rounding each move on its own loses every small one - a hand creeping
   * across the desk reports nothing at all - and slow careful movement is exactly what somebody
   * calibrating a pointer is doing.
   */
  private double restX;
  private double restY;

  private Speccy plugged;
  private KempstonMousePeripheral mouse;

  /** Where the pointer was last seen on the picture, to take the difference from. */
  private Point wasAt;

  private final MouseMotionAdapter moving = new MouseMotionAdapter() {
    @Override
    public void mouseMoved(MouseEvent moved) {
      report(moved);
    }

    @Override
    public void mouseDragged(MouseEvent dragged) {
      report(dragged);
    }
  };

  private final MouseAdapter pressing = new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent pressed) {
      button(pressed, true);
    }

    @Override
    public void mouseReleased(MouseEvent released) {
      button(released, false);
    }

    @Override
    public void mouseExited(MouseEvent left) {
      // Off the picture the difference from the last place is meaningless: coming back in at
      // the far side would be one enormous move rather than the pointer arriving there. Held,
      // it never leaves, which is the point of holding it.
      wasAt = null;
    }
  };

  public MouseInternalFrame(Function<JInternalFrame, Speccy> machineOf) {
    super("Kempston Mouse");
    this.machineOf = machineOf;
    setSize(300, 220);

    sensitivity.setPreferredSize(new Dimension(110, 24));
    sensitivity.setToolTipText("How far the machine's pointer goes for the same hand movement");
    sensitivity.addChangeListener(e -> say());
    hold.setToolTipText("Keep the pointer in the machine's picture, so the mouse has no edges"
        + " - the middle button lets go, as it does in Fuse");
    hold.addActionListener(e -> held(hold.isSelected()));
    controls.add(hold);
    record.setToolTipText("Write down what the hand does and what the program reads, together");
    record.addActionListener(e -> recording(record.isSelected()));
    controls.add(record);
    controls.add(new JLabel("Sensitivity"));
    controls.add(sensitivity);
    controls.add(reading);
    assemble(counters);
    setCompact(false);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> {
      counters.repaint();
      say();
    });
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
        watch(getMachineWindow(), false);
        connect(plugged, false);
      }
    });
    say();
  }

  /**
   * Clipped onto a machine, or off it, which is the mouse being plugged into that machine's
   * expansion port or taken out of it.
   */
  @Override
  protected void attachmentChanged() {
    Speccy machine = isAttached() ? machineOf.apply(getMachineWindow()) : null;
    if (machine == plugged) {
      return;
    }
    connect(plugged, false);
    plugged = machine;
    mouse = machine == null ? null
        : (KempstonMousePeripheral) machine.peripherals.find(KempstonMousePeripheral.class);
    watch(getMachineWindow(), machine != null);
    if (machine == null && held) {
      hold.setSelected(false);
      held = false;
    }
    connect(plugged, true);
    say();
  }

  @Override
  protected void machineClosed() {
    watch(getMachineWindow(), false);
    connect(plugged, false);
    plugged = null;
    mouse = null;
    super.machineClosed();
  }

  /** Said on the emulator's own thread: switching a device on rebuilds the machine's ports. */
  private void connect(Speccy machine, boolean connected) {
    if (machine == null || mouse == null) {
      return;
    }
    KempstonMousePeripheral wired = mouse;
    machine.z80.later(() -> {
      wired.plugIn(connected);
      machine.peripherals.update();
    });
  }

  /**
   * Listens to the machine's picture, because that is where the mouse is used.
   * <p>
   * Not to this window: pointing at a window of numbers is not pointing at anything. The mouse
   * belongs to the machine, so it is moved over the machine.
   */
  private void watch(JInternalFrame window, boolean wanted) {
    if (!(window instanceof EmulatorInternalFrame machine)) {
      return;
    }
    JComponent picture = machine.emulatorCore.getPanel();
    picture.removeMouseMotionListener(moving);
    picture.removeMouseListener(pressing);
    wasAt = null;
    if (wanted) {
      picture.addMouseMotionListener(moving);
      picture.addMouseListener(pressing);
    }
  }

  /**
   * How far the pointer moved since it was last seen, which is what the mouse reports.
   * <p>
   * A difference and not a position: the counters wrap, and the program on the other side adds
   * up the differences itself. Taking the position would tie the machine's pointer to the size
   * of this window's picture, which the machine knows nothing about.
   */
  private void report(MouseEvent where) {
    if (mouse == null) {
      return;
    }
    Point now = where.getPoint();
    if (held) {
      // Fuse's line: the movement is measured from the middle, and then the pointer is put
      // back in the middle, so the next movement is measured from there too and the hand can
      // go on in one direction for ever.
      Component picture = where.getComponent();
      Point middle = new Point(picture.getWidth() / 2, picture.getHeight() / 2);
      if (now.equals(middle)) {
        return;
      }
      wasAt = middle;
      putBackInTheMiddle(picture, middle);
    }
    if (wasAt != null) {
      double scale = sensitivity.getValue() / 100.0;
      double dx = (now.x - wasAt.x) * scale + restX;
      double dy = (now.y - wasAt.y) * scale + restY;
      int wholeX = (int) dx, wholeY = (int) dy;
      restX = dx - wholeX;
      restY = dy - wholeY;
      mouse.mouse().moved(wholeX, wholeY);
    }
    wasAt = now;
  }

  private void button(MouseEvent which, boolean down) {
    // The middle button lets go, and takes hold again, exactly as it does in Fuse: it is the
    // one button a Kempston Mouse never had, so it is free to mean something to the emulator.
    if (which.getButton() == MouseEvent.BUTTON2 && !down) {
      hold.setSelected(!hold.isSelected());
      held(hold.isSelected());
      return;
    }
    if (mouse == null) {
      return;
    }
    // The mouse on the desk has its buttons in the order the machine expects: left, right, then
    // the wheel if it is one that can be pressed.
    int number = switch (which.getButton()) {
      case MouseEvent.BUTTON1 -> 0;
      case MouseEvent.BUTTON3 -> 1;
      case MouseEvent.BUTTON2 -> 2;
      default -> -1;
    };
    if (number >= 0) {
      mouse.mouse().button(number, down);
    }
  }

  private void recording(boolean wanted) {
    if (mouse == null) {
      record.setSelected(false);
      return;
    }
    if (wanted) {
      recording.start();
      mouse.mouse().watch(recording);
    } else {
      mouse.mouse().watch(null);
      JOptionPane.showMessageDialog(this, recording.report(), "What went over the wire",
          JOptionPane.INFORMATION_MESSAGE);
    }
    say();
  }

  /**
   * Both ends of the wire, written down with the time, and then crossed.
   * <p>
   * What the hand did is known here; what the program made of it is only visible in when it
   * asked and what it got. The number that decides whether a pointer behaves is the largest
   * step between two consecutive readings: a program works out how far to move from the
   * difference between what it reads now and what it read last, as a signed byte, so a step of
   * more than 127 is read as a large move BACKWARDS. That is a pointer that jumps about no
   * matter how steadily the hand moves.
   */
  private static class Recording implements KempstonMouse.Watcher {

    private long began;
    private int hand;
    private int counts;
    private int reads;
    private int biggestStep;
    private int stepsOver127;

    /**
     * Counts that have gone by since the program last looked.
     * <p>
     * This is the crossing of the two ends, and it cannot be had from either alone: the counter
     * itself only ever shows a difference of at most 127 either way, so by the time a step is
     * too big to read the evidence of it is gone. Knowing what the hand did is what makes the
     * real size of the step visible.
     */
    private int sinceItLooked;

    void start() {
      began = System.currentTimeMillis();
      hand = counts = reads = biggestStep = stepsOver127 = sinceItLooked = 0;
    }

    @Override
    public void handMoved(int dx, int dy, int x, int y) {
      hand += Math.abs(dx);
      counts += Math.abs(dx);
      sinceItLooked += Math.abs(dx);
    }

    @Override
    public void programRead(String which, int value) {
      if (!"x".equals(which)) {
        return;
      }
      reads++;
      biggestStep = Math.max(biggestStep, sinceItLooked);
      if (sinceItLooked > 127) {
        stepsOver127++;
      }
      sinceItLooked = 0;
    }

    String report() {
      double seconds = Math.max(0.001, (System.currentTimeMillis() - began) / 1000.0);
      return String.format(
          "Over %.1f seconds:%n%n"
              + "  the hand moved %d pixels sideways%n"
              + "  which sent %d counts%n%n"
              + "  the program read the horizontal counter %d times, %.0f a second%n"
              + "  the most counts that went by between two of its readings: %d%n"
              + "  times more than 127 went by unseen: %d%n%n"
              + "%s",
          seconds, hand, counts, reads, reads / seconds, biggestStep, stepsOver127,
          stepsOver127 > 0
              ? "A step over 127 is read as a large move BACKWARDS, because a program works out\n"
              + "how far to go from the difference as a signed byte. That is the pointer jumping\n"
              + "about however steadily the hand moves. Lower the sensitivity until this is zero."
              : reads == 0
                  ? "The program never read the mouse at all while this was recording."
                  : "No reading was more than 127 from the one before it, so nothing here would\n"
                  + "make a pointer jump backwards.");
    }
  }

  /** Holds the pointer in the picture, or lets it go, hiding it while it is held. */
  private void held(boolean wanted) {
    held = wanted;
    wasAt = null;
    if (!(getMachineWindow() instanceof EmulatorInternalFrame machine)) {
      return;
    }
    JComponent picture = machine.emulatorCore.getPanel();
    picture.setCursor(wanted ? hidden() : Cursor.getDefaultCursor());
    if (wanted) {
      putBackInTheMiddle(picture, new Point(picture.getWidth() / 2, picture.getHeight() / 2));
    }
    say();
  }

  private boolean held;
  private Robot warp;

  /**
   * Moves the pointer back to that place on the screen.
   * <p>
   * Not every desktop lets a program move the pointer. Where it does not, holding simply does
   * not hold and the mouse goes on working as it did, which is better than refusing to run.
   */
  private void putBackInTheMiddle(Component picture, Point middle) {
    try {
      if (warp == null) {
        warp = new Robot();
      }
      Point on = picture.getLocationOnScreen();
      warp.mouseMove(on.x + middle.x, on.y + middle.y);
    } catch (Exception cannot) {
      held = false;
      hold.setSelected(false);
    }
  }

  /** Held, the pointer on the desk is not where anything is: showing it would be a second one. */
  private static Cursor hidden() {
    return Toolkit.getDefaultToolkit().createCustomCursor(
        new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
        new Point(), "held");
  }

  private void say() {
    reading.setText(mouse == null ? "not plugged into a machine"
        : String.format("%d%%%s", sensitivity.getValue(),
            hold.isSelected() ? " - held, middle button lets go" : ""));
  }

  @Override
  protected String expandTip() {
    return "Show what the machine is being told, or just the line";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the mouse in";
  }

  /**
   * The two counters and the three buttons, drawn as the machine reads them.
   * <p>
   * The dot is not where the pointer is - nothing here knows that. It is the pair of counters
   * seen as a place, which is enough to tell a mouse that is being heard from a mouse that is
   * not, and that is the question somebody has when a program draws nothing.
   */
  private class Counters extends JComponent {

    Counters() {
      setBackground(new Color(0x101418));
      setPreferredSize(new Dimension(160, 120));
    }

    @Override
    protected void paintComponent(Graphics pen) {
      int width = getWidth(), height = getHeight();
      pen.setColor(getBackground());
      pen.fillRect(0, 0, width, height);
      if (mouse == null) {
        return;
      }
      KempstonMouse reading = mouse.mouse();

      pen.setColor(new Color(0x1E2A33));
      pen.drawRect(0, 0, width - 1, height - 24);
      int x = reading.x() * (width - 8) / 256;
      int y = (255 - reading.y()) * (height - 32) / 256;
      pen.setColor(new Color(0x5FD08A));
      pen.fillOval(x, y, 8, 8);

      pen.drawString(String.format("x %3d   y %3d", reading.x(), reading.y()), 6, height - 6);
      for (int button = 0; button < 3; button++) {
        pen.setColor(reading.isHeld(button) ? new Color(0x5FD08A) : new Color(0x1E2A33));
        pen.fillRect(width - 46 + button * 14, height - 16, 10, 10);
      }
    }
  }
}

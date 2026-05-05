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

package com.fpetrola.oozx.speccy.devices.mouse;

import com.fpetrola.oozx.speccy.devices.DeviceFrame;
import com.fpetrola.oozx.speccy.windows.KnobRows;
import com.fpetrola.oozx.speccy.windows.MachineWindow;
import com.fpetrola.oozx.speccy.screen.Knob;

import java.util.List;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Kempston Mouse control panel: the physical mouse drives the machine's picture directly, and
 * this window only visualises the counters and buttons a program actually receives.
 */
public class MouseInternalFrame extends DeviceFrame<KempstonMousePeripheral> {

  private static final int REFRESH_MILLIS = 50;

  private final Counters counters = new Counters();
  private final JLabel reading = new JLabel();

  /**
   * Counts sent per pixel of hand movement, as a fraction (0.70 = 70%). No fixed scale is
   * correct since programs and window sizes vary; measured default: normal hand speed produced
   * 175 counts between reads at a 50Hz poll rate, and a signed byte can only carry 127, so 0.70
   * (122 counts) keeps every reading under that limit.
   */
  private double acrossTheDesk = 0.70;
  private double upTheDesk = 0.70;
  private boolean swapButtons;

  /** Recentres the cursor after each move so the picture's edges never limit hand travel. */
  private final JToggleButton hold = new JToggleButton("Hold");

  /** Toggles logging of both the raw hand motion and what the program reads back. */
  private final JToggleButton record = new JToggleButton("Record");

  private final Recording recording = new Recording();

  /** Fractional remainder carried between moves; without it, sub-pixel drags round to zero. */
  private double restX;
  private double restY;

  private KempstonMousePeripheral mouse;

  /** Last observed cursor position, the baseline for the next delta. */
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
      // Discards the baseline: re-entering elsewhere must not read as one huge jump.
      wasAt = null;
    }
  };

  public MouseInternalFrame() {
    super("Kempston Mouse", KempstonMousePeripheral.class);

    hold.setToolTipText("Keep the pointer in the machine's picture, so the mouse has no edges"
        + " - the middle button lets go");
    hold.addActionListener(e -> held(hold.isSelected()));
    controls.add(hold);
    record.setToolTipText("Write down what the hand does and what the program reads, together");
    record.addActionListener(e -> recording(record.isSelected()));
    controls.add(record);
    controls.add(reading);

    JPanel inside = new JPanel(new BorderLayout(8, 0));
    JPanel turning = new KnobRows(this::say).of(knobs());
    // A fixed width stops the knob panel from expanding to consume the whole frame.
    turning.setPreferredSize(new Dimension(320, turning.getPreferredSize().height));
    inside.add(turning, BorderLayout.WEST);
    inside.add(counters, BorderLayout.CENTER);
    assemble(inside);
    setCompact(false);
    setSize(600, 320);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> {
      counters.repaint();
      say();
    });
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    say();
  }

  @Override
  protected void plugged(KempstonMousePeripheral device) {
    mouse = device;
    watch(getMachineWindow(), device != null);
    if (device == null && held) {
      hold.setSelected(false);
      held = false;
    }
    say();
  }

  /** Attaches motion/click listeners to the machine's own picture, not this panel. */
  private void watch(JInternalFrame window, boolean wanted) {
    if (!(window instanceof MachineWindow machine)) {
      return;
    }
    JComponent picture = machine.picture();
    picture.removeMouseMotionListener(moving);
    picture.removeMouseListener(pressing);
    wasAt = null;
    if (wanted) {
      picture.addMouseMotionListener(moving);
      picture.addMouseListener(pressing);
    }
  }

  /** Reports a movement delta, not an absolute position: the wrapping counters only ever
   * carry differences, independent of this window's picture size. */
  private void report(MouseEvent where) {
    if (mouse == null) {
      return;
    }
    Point now = where.getPoint();
    if (held) {
      // Re-centring after each read lets travel continue indefinitely in one direction.
      Component picture = where.getComponent();
      Point middle = new Point(picture.getWidth() / 2, picture.getHeight() / 2);
      if (now.equals(middle)) {
        return;
      }
      wasAt = middle;
      putBackInTheMiddle(picture, middle);
    }
    if (wasAt != null) {
      double dx = (now.x - wasAt.x) * acrossTheDesk + restX;
      double dy = (now.y - wasAt.y) * upTheDesk + restY;
      int wholeX = (int) dx, wholeY = (int) dy;
      restX = dx - wholeX;
      restY = dy - wholeY;
      mouse.mouse().moved(wholeX, wholeY);
    }
    wasAt = now;
  }

  private void button(MouseEvent which, boolean down) {
    // The real Kempston Mouse has no middle button, so it is repurposed as the hold toggle.
    if (which.getButton() == MouseEvent.BUTTON2 && !down) {
      hold.setSelected(!hold.isSelected());
      held(hold.isSelected());
      return;
    }
    if (mouse == null) {
      return;
    }
    // Button numbering matches what the machine expects: left, right, then the wheel button.
    int number = switch (which.getButton()) {
      case MouseEvent.BUTTON1 -> swapButtons ? 1 : 0;
      case MouseEvent.BUTTON3 -> swapButtons ? 0 : 1;
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
   * Logs raw hand motion against program reads to find steps a signed-byte counter cannot
   * represent: anything over 127 between two reads wraps to a large negative move.
   */
  private static class Recording implements KempstonMouse.Watcher {

    private long began;
    private int hand;
    private int counts;
    private int reads;
    private int biggestStep;
    private int stepsOver127;

    /** True hand movement since the last read, tracked separately because the 127-limited
     * counter itself cannot reveal an overflow after the fact. */
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

  /** Enables or disables the recentring hold, hiding the cursor while active. */
  private void held(boolean wanted) {
    held = wanted;
    wasAt = null;
    if (!(getMachineWindow() instanceof MachineWindow machine)) {
      return;
    }
    JComponent picture = machine.picture();
    picture.setCursor(wanted ? hidden() : Cursor.getDefaultCursor());
    if (wanted) {
      putBackInTheMiddle(picture, new Point(picture.getWidth() / 2, picture.getHeight() / 2));
    }
    say();
  }

  private boolean held;
  private Robot warp;

  /** Warps the OS pointer to a screen point; if the platform refuses, hold is switched off
   * instead of failing. */
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

  /** A fully transparent cursor, used so the real pointer does not duplicate the on-screen dot. */
  private static Cursor hidden() {
    return Toolkit.getDefaultToolkit().createCustomCursor(
        new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
        new Point(), "held");
  }

  private void say() {
    reading.setText(mouse == null ? "not plugged into a machine"
        : hold.isSelected() ? "held - the middle button lets go" : "over the machine's picture");
  }

  /** This device's tunables, expressed as {@link Knob}s so the shared knob-panel layout
   * renders them without any device-specific UI code. */
  private List<Knob> knobs() {
    return List.of(
        Knob.number("across", "Sideways", "How far the machine's pointer goes for the same"
                + " movement of the hand, across. More than a hundred is the pointer moving"
                + " further than your hand does.",
            "Movement", 0.1, 8, 0.05, 0.70,
            () -> acrossTheDesk, value -> acrossTheDesk = asDouble(value)),
        Knob.number("up", "Up and down", "The same for the other direction. A Spectrum screen is"
                + " wider than it is tall, so the two are not always the same number.",
            "Movement", 0.1, 8, 0.05, 0.70,
            () -> upTheDesk, value -> upTheDesk = asDouble(value)),
        Knob.switching("carry", "Carry over", "A stroke too quick to be read at once arrives at"
                + " the next reading instead of being lost. Off, it is dropped and the pointer"
                + " stays under your hand rather than catching up behind it.",
            "Movement", true,
            () -> mouse == null || mouse.mouse().isCarryOver(),
            value -> {
              if (mouse != null) {
                mouse.mouse().setCarryOver(Boolean.parseBoolean(String.valueOf(value)));
              }
            }),
        Knob.switching("swap", "Swap the buttons", "The right button becomes the machine's first"
                + " one, for people who hold a mouse the other way.",
            "Buttons", false,
            () -> swapButtons, value -> swapButtons = Boolean.parseBoolean(String.valueOf(value))));
  }

  private static double asDouble(Object value) {
    return value instanceof Number number ? number.doubleValue()
        : Double.parseDouble(String.valueOf(value));
  }

  @Override
  protected String expandTip() {
    return "Show what the machine is being told, or just the line";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the mouse in";
  }

  /** Plots the raw x/y counters as a dot, plus the three button states - not the OS cursor,
   * which this panel has no access to. */
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

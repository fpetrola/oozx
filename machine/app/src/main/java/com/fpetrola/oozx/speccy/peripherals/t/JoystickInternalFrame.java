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

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A window on the joystick of the machine in front: which way it is pushed and whether fire is
 * down, as that machine's Kempston port reads them, and which gamepad is doing it. Where to look
 * when a game does not react: it says whether a pad was found at all, whether the machine has its
 * Kempston interface on, and whether what is pressed gets through.
 */
public class JoystickInternalFrame extends AttachedFrame {
  private static final int REFRESH_MILLIS = 33;
  static final int RIGHT = 0x01, LEFT = 0x02, DOWN = 0x04, UP = 0x08, FIRE = 0x10;
  private final Function<JInternalFrame, Speccy> machineOf;
  private final Supplier<String> gamepad;
  private final JLabel reading = new JLabel();
  private final Stick stick = new Stick();

  public JoystickInternalFrame(Function<JInternalFrame, Speccy> machineOf, Supplier<String> gamepad) {
    super("Joystick");
    this.machineOf = machineOf;
    this.gamepad = gamepad;
    setSize(300, 220);
    controls.add(reading);
    assemble(stick);
    setCompact(false);
    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    refresh();
  }

  @Override
  protected String expandTip() {
    return "Show the stick, or just the reading";
  }

  @Override
  protected String attachTip() {
    return "Keep this under the machine's window, the same width as it";
  }

  private Speccy machine() {
    return isAttached() ? machineOf.apply(getMachineWindow()) : null;
  }

  /** What the machine's Kempston port answers right now, bit by bit. */
  int pressed() {
    Speccy machine = machine();
    return machine == null ? 0 : machine.joystick.kempstonRead(0x1f, new byte[1]) & 0xff;
  }

  String reading() {
    return reading.getText();
  }

  void refresh() {
    String pad = gamepad.get();
    Speccy machine = machine();
    setTitle("Joystick: " + (pad == null ? "no gamepad" : pad));
    reading.setText(machine == null ? "No machine" : machine.settings.current.joyKempston ? "Kempston on" : "Kempston off");
    stick.show(pressed());
  }

  /** Four arrows and a fire button, lit while the port says they are pressed. */
  private static class Stick extends JComponent {
    private int pressed;

    void show(int now) {
      if (now != pressed) {
        pressed = now;
        repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D pen = (Graphics2D) g;
      pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int size = Math.min(getWidth(), getHeight()) / 2 - 8;
      int cx = getWidth() / 2 - size / 2, cy = getHeight() / 2;
      arrow(pen, cx, cy - size, 0, -1, size / 2, (pressed & UP) != 0);
      arrow(pen, cx, cy + size, 0, 1, size / 2, (pressed & DOWN) != 0);
      arrow(pen, cx - size, cy, -1, 0, size / 2, (pressed & LEFT) != 0);
      arrow(pen, cx + size, cy, 1, 0, size / 2, (pressed & RIGHT) != 0);
      pen.setColor((pressed & FIRE) != 0 ? Color.RED : Color.GRAY);
      pen.fillOval(cx + size + size / 2 + size / 4, cy - size / 2, size, size);
    }

    private static void arrow(Graphics2D pen, int x, int y, int dx, int dy, int half, boolean lit) {
      int[] xs = {x + dx * half, x - dx * half + dy * half, x - dx * half - dy * half};
      int[] ys = {y + dy * half, y - dy * half - dx * half, y - dy * half + dx * half};
      pen.setColor(lit ? Color.GREEN : Color.GRAY);
      pen.fillPolygon(xs, ys, 3);
    }
  }
}

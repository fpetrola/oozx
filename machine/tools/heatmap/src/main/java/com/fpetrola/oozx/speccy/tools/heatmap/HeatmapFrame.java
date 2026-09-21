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


package com.fpetrola.oozx.speccy.tools.heatmap;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * The 64K as a picture: one pixel per byte, 256 to a row, the brighter the more code ran there.
 * <p>
 * A game's own code is a few hundred bytes of a memory that is mostly screen, buffers and
 * nothing at all, and where those bytes are is the first thing anybody taking a game apart
 * wants to know. This says it at a glance and without disassembling anything: whatever lights
 * up is code, and whatever lights up while you are playing is the code doing what you are
 * watching it do.
 */
public class HeatmapFrame extends MachineFrame {

  private static final int SIDE = 256;
  private static final int REFRESH_MILLIS = 100;

  /** Where the three parts of a 48K machine's address space begin, for the picture to be read against. */
  private static final int[] EDGES = {0x4000, 0x5b00, 0x8000, 0xc000};

  private final BufferedImage picture =
      new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
  private final JToggleButton recently = new JToggleButton("Lately", true);
  private final JLabel said = new JLabel(" ");
  private final Memory drawn = new Memory();
  private Heat heat;
  private int pointingAt = -1;

  public HeatmapFrame() {
    super("Heatmap");
    setSize(560, 620);

    recently.setToolTipText("What ran in the last moment, or everything that has ever run");
    recently.addActionListener(e -> {
      recently.setText(recently.isSelected() ? "Lately" : "Ever");
      drawn.repaint();
    });
    JButton forget = new JButton("Forget");
    forget.setToolTipText("Throw away what has run so far");
    forget.addActionListener(e -> {
      if (heat != null) {
        heat.forget();
      }
    });
    controls.add(recently);
    controls.add(forget);
    controls.add(javax.swing.Box.createHorizontalStrut(10));
    controls.add(said);

    assemble(drawn);
    setCompact(false);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> {
      if (heat != null) {
        heat.fade();
      }
      drawn.repaint();
      say();
    });
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    if (heat != null) {
      heat.close();
      heat = null;
    }
    if (now != null) {
      heat = new Heat(now);
    }
    say();
  }

  @Override
  protected String expandTip() {
    return "Show the picture, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  /** What the address under the pointer is, and how much of the memory has ever run. */
  private void say() {
    if (heat == null) {
      said.setText("not clipped onto a machine");
      return;
    }
    String whole = "%d of 65536 bytes have run".formatted(heat.placesThatRan());
    said.setText(pointingAt < 0 ? whole
        : "%04X in %s   ×%d   %s".formatted(pointingAt, where(pointingAt),
            heat.ever(pointingAt), whole));
  }

  /** Which part of the machine an address falls in, said the way somebody reading a map says it. */
  private static String where(int address) {
    if (address < EDGES[0]) {
      return "ROM";
    }
    return address < EDGES[1] ? "the screen" : "RAM";
  }

  /**
   * The picture itself. Scaled with no smoothing on purpose: a byte is a pixel, and a routine
   * eight bytes long has to stay eight pixels rather than become a smudge.
   */
  private class Memory extends JComponent {

    Memory() {
      setPreferredSize(new Dimension(SIDE * 2, SIDE * 2));
      MouseAdapter pointing = new MouseAdapter() {
        public void mouseMoved(MouseEvent e) {
          pointingAt = addressAt(e);
          say();
        }

        public void mouseExited(MouseEvent e) {
          pointingAt = -1;
          say();
        }
      };
      addMouseMotionListener(pointing);
      addMouseListener(pointing);
    }

    private int addressAt(MouseEvent e) {
      int x = e.getX() * SIDE / Math.max(1, getWidth());
      int y = e.getY() * SIDE / Math.max(1, getHeight());
      return x < 0 || x >= SIDE || y < 0 || y >= SIDE ? -1 : y << 8 | x;
    }

    @Override
    protected void paintComponent(Graphics g) {
      boolean lately = recently.isSelected();
      long hottest = heat == null ? 0 : heat.hottest(lately);
      for (int address = 0; address < 0x10000; address++) {
        long times = heat == null ? 0 : lately ? heat.lately(address) : heat.ever(address);
        picture.setRGB(address & 0xff, address >>> 8, warmth(times, hottest));
      }
      Graphics2D into = (Graphics2D) g;
      into.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      into.drawImage(picture, 0, 0, getWidth(), getHeight(), null);
      into.setColor(new Color(0x40, 0x80, 0xff, 0x60));
      for (int edge : EDGES) {
        int y = (edge >>> 8) * getHeight() / SIDE;
        into.drawLine(0, y, getWidth(), y);
      }
    }
  }

  /**
   * How hot a count looks, on a log scale: the difference worth seeing is between a place that
   * ran once and one that ran a thousand times, and on a straight scale the inner loop is the
   * only thing on the picture.
   */
  static int warmth(long times, long hottest) {
    if (times == 0) {
      return 0x101014;
    }
    double heat = Math.log1p(times) / Math.log1p(Math.max(times, hottest));
    int red = (int) Math.min(255, heat * 3 * 255);
    int green = (int) Math.min(255, Math.max(0, (heat * 3 - 1) * 255));
    int blue = (int) Math.min(255, Math.max(0, (heat * 3 - 2) * 255));
    return red << 16 | green << 8 | blue;
  }
}

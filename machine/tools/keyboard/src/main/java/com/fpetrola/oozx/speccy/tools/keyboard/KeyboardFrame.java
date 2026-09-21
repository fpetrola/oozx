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

package com.fpetrola.oozx.speccy.tools.keyboard;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.keyboard.KeyMatrix;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey.*;

/**
 * A window on the keyboard of the machine in front: a photograph of a 48K, with every key that is
 * down on the machine shown pressed into the picture.
 * <p>
 * Where to look when the typing does not arrive: it shows the matrix the machine itself reads, so
 * a key lit here and ignored by the game is the game's business, and one that never lights is the
 * host's key never getting this far. It is also the whole keyboard a Spectrum has when whoever is
 * at it has a PC keyboard instead - which key of the machine a shifted key of theirs turns into.
 */
public class KeyboardFrame extends MachineFrame {
  private static final int REFRESH_MILLIS = 33;

  /*
   * The well a key sinks into. The case between the keys samples rgb(26, 27, 30) - not black, as
   * it looked - and a well is that in shadow, darkest along its top wall because the picture is
   * lit from above: the top strip of a key top reads 110.6 against 104.7 along the bottom.
   */
  private static final Color WELL_TOP = new Color(12, 13, 15);
  private static final Color WELL_BOTTOM = new Color(24, 25, 28);
  /** How much the key darkens once it is down there. */
  private static final Color SHADE = new Color(0, 0, 0, 80);

  /*
   * What a key does when it is pressed, measured off the photograph rather than assumed.
   *
   * A key is a block standing on the case, and the camera sees the one wall of it that faces the
   * camera's axis. Along the Q, A and Z rows, where the run of wall grey touching the key is wall
   * and not legend, which wall that is changes sign across the picture - the keys on the left
   * show their right wall and the ones on the right their left - crossing over between the fifth
   * and the sixth key. That is where the axis falls. The wall runs five pixels wide out at the
   * edges and nothing in the middle, and one pixel at the ends everywhere.
   *
   * So pressing a key is one thing, not two: its lit face slides down that wall to where its base
   * was, which is its own wall's width towards the axis and a pixel up the picture, and the walls
   * themselves go inside the case where nothing can see them. The face keeps its size - the base
   * is a few millimetres further from a camera the better part of a metre away, which is under a
   * pixel - so there is no shrinking to model. Shrinking it instead, as this did, kept the walls
   * in the picture and read as a key getting smaller rather than one going down.
   *
   * The keys are also all the same size in every row - 73, 72, 72, 72 wide and 53, 55, 54, 55
   * high - so there is no convergence up the picture to model either.
   */
  private static final double AXIS_X = 582;
  /** How wide a key's wall is, per pixel of its distance from the axis: five pixels out at the edges. */
  private static final double WALL_PER_PIXEL = 0.010;
  /** How far up the picture the base sits from the face, which the rows without a legend over them put at one. */
  private static final int WALL_ENDS = 1;
  /** The far wall, the one no camera sees, which is still a pixel of key the hole has to take. */
  private static final int WALL_AWAY = 1;
  /** Room over and under, where the key's edge fades out but no wall shows. */
  private static final int EDGE_ENDS = 2;
  /** The key's outer edge fades into the case over about a pixel, which the hole takes as well. */
  private static final int EDGE_BLEED = 1;
  /** The corner a key is rounded by, measured at six pixels; the hole rounds by one more to swallow it. */
  private static final int CORNER = 7;

  private static final BufferedImage PICTURE = load();

  /**
   * Where each key sits on the photograph, in its pixels, measured off the picture itself rather
   * than laid out on a grid: the rows are evenly pitched but the picture is not square to it.
   */
  private static final Map<SpectrumKey, Rectangle> ON_PICTURE = new EnumMap<>(SpectrumKey.class);

  static {
    SpectrumKey[][] rows = {
        {ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, ZERO},
        {Q, W, E, R, T, Y, U, I, O, P},
        {A, S, D, F, G, H, J, K, L, ENTER},
        {CAPS_SHIFT, Z, X, C, V, B, N, M, SYMBOL_SHIFT, SPACE}};
    int[][] lefts = {
        {35, 140, 244, 349, 453, 557, 660, 764, 870, 975},
        {87, 191, 296, 401, 505, 608, 713, 818, 924, 1029},
        {113, 217, 321, 426, 531, 634, 740, 845, 950, 1056},
        {31, 164, 268, 373, 478, 582, 688, 793, 898, 1004}};
    int[] tops = {287, 392, 495, 599};
    int[] heights = {53, 55, 54, 55};

    for (int row = 0; row < rows.length; row++) {
      for (int i = 0; i < rows[row].length; i++) {
        SpectrumKey key = rows[row][i];
        int width = key == CAPS_SHIFT ? 99 : key == SPACE ? 125 : 73;
        ON_PICTURE.put(key, new Rectangle(lefts[row][i], tops[row], width, heights[row]));
      }
    }
  }

  /** The keys a click holds down rather than taps, because they are only ever held with another. */
  private static final EnumSet<SpectrumKey> STICKY = EnumSet.of(CAPS_SHIFT, SYMBOL_SHIFT);

  private final EnumSet<SpectrumKey> stuck = EnumSet.noneOf(SpectrumKey.class);
  private final Keys keys = new Keys();

  public KeyboardFrame() {
    super("Keyboard");
    setSize(620, 420);
    prefersDock(Dock.BOTTOM);
    assemble(keys);
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
    return "Show the keyboard, or just the title";
  }

  @Override
  protected String attachTip() {
    return "Keep this under the machine's window, the same width as it";
  }

  private static BufferedImage load() {
    try (InputStream in = KeyboardFrame.class.getResourceAsStream("/images/spectrum-front2.png")) {
      return ImageIO.read(in);
    } catch (Exception missing) {
      throw new IllegalStateException("the keyboard picture is not on the classpath", missing);
    }
  }

  /** Where that key is on the picture, so that a key left off the table is a test failure and not a key that never lights. */
  static Rectangle placeOf(SpectrumKey key) {
    return ON_PICTURE.get(key);
  }

  /** How wide the wall this key shows is, which is also exactly how far it travels when pressed. */
  static int wallOf(Rectangle top) {
    return (int) Math.round(Math.abs(top.getCenterX() - AXIS_X) * WALL_PER_PIXEL);
  }

  /**
   * The whole key on the picture, which is more than the lit face the table holds: the wall it
   * shows on the side that looks at the axis, and a pixel of the far one. All of this goes inside
   * the case when the key is pressed, so all of it is what the hole has to cover.
   */
  static Rectangle keyOn(Rectangle top) {
    boolean wallOnTheRight = top.getCenterX() < AXIS_X;
    int wall = wallOf(top);
    return new Rectangle(top.x - (wallOnTheRight ? WALL_AWAY : wall), top.y - EDGE_ENDS,
        top.width + wall + WALL_AWAY, top.height + 2 * EDGE_ENDS);
  }

  /** Where a key's lit face lands once it is down: on its base, which is its own wall away. */
  static Rectangle sunk(Rectangle top) {
    Rectangle down = new Rectangle(top);
    down.translate(top.getCenterX() < AXIS_X ? wallOf(top) : -wallOf(top), -WALL_ENDS);
    return down;
  }

  /** The hole a key leaves behind: the whole of it, its faded edge, and wherever its face went. */
  static Rectangle holeFor(Rectangle top) {
    Rectangle hole = keyOn(top);
    hole.grow(EDGE_BLEED, EDGE_BLEED);
    return hole.union(sunk(top));
  }

  /** The key under that point of the picture, or null where the person clicked the case. */
  static SpectrumKey at(int x, int y) {
    for (Map.Entry<SpectrumKey, Rectangle> each : ON_PICTURE.entrySet()) {
      if (each.getValue().contains(x, y)) {
        return each.getKey();
      }
    }
    return null;
  }

  /** Which keys the machine reads as held right now. */
  EnumSet<SpectrumKey> held() {
    EnumSet<SpectrumKey> down = EnumSet.noneOf(SpectrumKey.class);
    Speccy machine = machine();
    if (machine != null) {
      KeyMatrix matrix = Input.of(machine).keyboard().matrix();
      for (SpectrumKey key : SpectrumKey.values()) {
        if (matrix.isDown(key)) {
          down.add(key);
        }
      }
    }
    return down;
  }

  /**
   * Clicking a key of the picture. One pointer cannot hold a shift and a key at once, so the two
   * shifts stay down once clicked and are let go of by the next key typed, or by clicking them
   * again - without which the whole symbol-shifted half of the keyboard is out of reach here.
   */
  void clicked(SpectrumKey key, boolean down) {
    if (STICKY.contains(key)) {
      if (down) {
        boolean wasStuck = stuck.remove(key);
        if (!wasStuck) {
          stuck.add(key);
        }
        type(key, !wasStuck);
      }
      return;
    }
    type(key, down);
    if (!down) {
      for (SpectrumKey shift : stuck) {
        type(shift, false);
      }
      stuck.clear();
    }
  }

  /** The shifts that are being held for whatever is typed next. */
  EnumSet<SpectrumKey> stuck() {
    return EnumSet.copyOf(stuck);
  }

  /** Typing on the picture: the key goes down on the machine as if somebody pressed the rubber. */
  void type(SpectrumKey key, boolean down) {
    Speccy machine = machine();
    if (machine == null) {
      return;
    }
    if (down) {
      Input.of(machine).keyboard().press(key);
    } else {
      Input.of(machine).keyboard().release(key);
    }
  }

  void refresh() {
    setTitle(machine() == null ? "Keyboard: no machine" : "Keyboard");
    keys.show(held());
  }

  /** The photograph, with the keys that are down pressed into it, and clickable. */
  private class Keys extends JComponent {
    private EnumSet<SpectrumKey> pressed = EnumSet.noneOf(SpectrumKey.class);
    /** The key the mouse is holding down, so that it is let go of wherever the button comes up. */
    private SpectrumKey holding;
    private double scale = 1;
    private int left;
    private int top;

    Keys() {
      MouseAdapter mouse = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          holding = keyAt(e.getPoint());
          if (holding != null) {
            clicked(holding, true);
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (holding != null) {
            clicked(holding, false);
            holding = null;
          }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          setCursor(Cursor.getPredefinedCursor(
              keyAt(e.getPoint()) == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        }
      };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
    }

    /** That point of the window, as a key of the picture underneath it. */
    private SpectrumKey keyAt(Point where) {
      return at((int) ((where.x - left) / scale), (int) ((where.y - top) / scale));
    }

    void show(EnumSet<SpectrumKey> now) {
      if (!now.equals(pressed)) {
        pressed = now;
        repaint();
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D pen = (Graphics2D) g;
      pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      scale = Math.min(getWidth() / (double) PICTURE.getWidth(), getHeight() / (double) PICTURE.getHeight());
      int width = (int) (PICTURE.getWidth() * scale), height = (int) (PICTURE.getHeight() * scale);
      left = (getWidth() - width) / 2;
      top = (getHeight() - height) / 2;
      pen.drawImage(PICTURE, left, top, width, height, null);

      for (SpectrumKey key : pressed) {
        Rectangle on = ON_PICTURE.get(key);
        Rectangle down = sunk(on), hole = holeFor(on);
        double round = Math.max(2, CORNER * scale);

        Shape well = new RoundRectangle2D.Double(screenX(hole.x), screenY(hole.y),
            hole.width * scale, hole.height * scale, round, round);
        pen.setPaint(new GradientPaint(0, screenY(hole.y), WELL_TOP, 0, screenY(hole.y + hole.height), WELL_BOTTOM));
        pen.fill(well);

        // clipped to the well, so that nothing of the key can stand outside the hole it is in
        Shape around = pen.getClip();
        pen.clip(well);
        pen.drawImage(PICTURE, screenX(down.x), screenY(down.y),
            screenX(down.x + down.width), screenY(down.y + down.height),
            on.x, on.y, on.x + on.width, on.y + on.height, null);
        pen.setColor(SHADE);
        pen.fill(new RoundRectangle2D.Double(screenX(down.x), screenY(down.y),
            down.width * scale, down.height * scale, round, round));
        pen.setClip(around);
      }
    }

    private int screenX(double onPicture) {
      return left + (int) Math.round(onPicture * scale);
    }

    private int screenY(double onPicture) {
      return top + (int) Math.round(onPicture * scale);
    }
  }
}

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

package com.fpetrola.oozx.speccy.devices.printer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * The paper coming out of a ZX Printer, drawn as the thing itself: a strip of aluminised paper
 * about four centimetres wide, with the dots burned through the silver to the black underneath.
 * <p>
 * The burning is done once, into an image with a fixed number of pixels per dot, and that image is
 * what gets scaled to whatever the zoom is. So the printout looks like a printout at every size -
 * a page seen small is a page seen small, not a bitmap - and zooming costs nothing but a blit.
 * <p>
 * The device's package brings its own view: everything about a printer is here, including what one
 * looks like, and a window elsewhere only has to find somewhere to put this.
 */
public class PrinterPaper extends JPanel {
  /** The aluminised surface, which is lighter along the middle of the roll and duller at the edges. */
  private static final Color SILVER_EDGE = new Color(0xb8, 0xbb, 0xb3);
  private static final Color SILVER_MID = new Color(0xdc, 0xde, 0xd7);
  private static final Color BURN = new Color(0x1c, 0x1a, 0x17);
  private static final Color EDGE = new Color(0x94, 0x97, 0x8f);

  private static final double MIN_ZOOM = 0.4;
  private static final double MAX_ZOOM = 16;
  private static final int MARGIN = 14;

  private Printout paper;
  private final Printout.Listener watching = row -> SwingUtilities.invokeLater(this::rowArrived);

  /** Screen pixels per dot. Anything, not just whole numbers: the paper is 256 dots wide either way. */
  private double zoom = 3;

  private BufferedImage strip;
  /** Pixels per dot the burned image was made at, which is not the zoom and follows it in steps. */
  private int burnedAt;

  private Point draggingFrom;

  /** Whether to draw a printout or the dots themselves, which is what a printer test wants to see. */
  private boolean filtered = true;

  public PrinterPaper(Printout paper) {
    setBackground(new Color(0x2e, 0x30, 0x2c));
    setPrintout(paper);
    setAutoscrolls(true);

    // Ctrl and the wheel zooms; the wheel on its own is scrolling, and a panel with a wheel
    // listener keeps the event to itself, so what is not a zoom is handed back to the scroll pane.
    addMouseWheelListener(wheel -> {
      if (wheel.isControlDown()) {
        zoomAbout(wheel.getPoint(), zoom * Math.pow(0.88, wheel.getPreciseWheelRotation()));
      } else if (getParent() != null) {
        getParent().dispatchEvent(SwingUtilities.convertMouseEvent(this, wheel, getParent()));
      }
    });

    MouseAdapter dragging = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent grabbed) {
        draggingFrom = grabbed.getPoint();
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }

      @Override
      public void mouseReleased(MouseEvent let) {
        draggingFrom = null;
        setCursor(Cursor.getDefaultCursor());
      }

      /** Dragging moves the paper under the window, which is what a hand on a printout does. */
      @Override
      public void mouseDragged(MouseEvent to) {
        if (draggingFrom == null) {
          return;
        }
        Rectangle shown = getVisibleRect();
        shown.x -= to.getX() - draggingFrom.x;
        shown.y -= to.getY() - draggingFrom.y;
        scrollRectToVisible(shown);
      }
    };
    addMouseListener(dragging);
    addMouseMotionListener(dragging);
  }

  /**
   * The roll this shows: the printer's, when the window is clipped to a machine, and an empty one
   * of its own when it is not. Watching it is how a row that is burned reaches the screen.
   */
  public void setPrintout(Printout printout) {
    if (printout == paper) {
      return;
    }
    if (paper != null) {
      paper.stopWatching(watching);
    }
    paper = printout;
    strip = null;
    printout.whenPrinted(watching);
    revalidate();
    repaint();
  }

  public Printout printout() {
    return paper;
  }

  public boolean isFiltered() {
    return filtered;
  }

  /**
   * Off, the paper and the burning go and the dots are drawn as they are: black where the printer
   * fired, white where it did not. It is the same printout - one is what came out of the machine,
   * the other is what the machine sent.
   */
  public void setFiltered(boolean filtered) {
    this.filtered = filtered;
    strip = null;
    repaint();
  }

  public double zoom() {
    return zoom;
  }

  public void setZoom(double wanted) {
    zoomAbout(new Point(getWidth() / 2, getHeight() / 2), wanted);
  }

  /** As much of the paper across as the window is wide, which is where a printout is easiest to read. */
  public void fitWidth() {
    int across = getVisibleRect().width;
    if (across > 2 * MARGIN) {
      setZoom((across - 2.0 * MARGIN) / Printout.WIDTH);
    }
  }

  /**
   * Zooms keeping whatever is under the pointer under the pointer, which is the only way a zoom
   * feels like moving closer rather than like the picture jumping somewhere else.
   */
  private void zoomAbout(Point pointer, double wanted) {
    double next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, wanted));
    if (next == zoom) {
      return;
    }
    Rectangle shown = getVisibleRect();
    double onPaperX = (pointer.x - left()) / zoom;
    double onPaperY = (pointer.y - MARGIN) / zoom;

    zoom = next;
    revalidate();
    getParent().doLayout();

    int keptX = (int) Math.round(left() + onPaperX * zoom) - (pointer.x - shown.x);
    int keptY = (int) Math.round(MARGIN + onPaperY * zoom) - (pointer.y - shown.y);
    scrollRectToVisible(new Rectangle(keptX, keptY, shown.width, shown.height));
    repaint();
  }

  private int left() {
    return Math.max(MARGIN, (int) ((getWidth() - Printout.WIDTH * zoom) / 2));
  }

  private void rowArrived() {
    strip = null;
    // Following the paper out of the printer, unless whoever is looking has scrolled away to read
    // something further up, in which case moving the view under them is rude.
    Rectangle shown = getVisibleRect();
    boolean atTheEnd = shown.y + shown.height >= getHeight() - (int) (2 * zoom) - 1;
    revalidate();
    repaint();
    if (atTheEnd) {
      SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(0, getHeight() - 1, 1, 1)));
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension((int) (Printout.WIDTH * zoom) + 2 * MARGIN,
        Math.max((int) (paper.height() * zoom) + 2 * MARGIN, 140));
  }

  @Override
  protected void paintComponent(Graphics pen) {
    super.paintComponent(pen);
    Graphics2D canvas = (Graphics2D) pen.create();
    paint(canvas, left(), MARGIN, (int) (Printout.WIDTH * zoom),
        Math.max((int) (paper.height() * zoom), 80));
    canvas.dispose();
  }

  /** The strip and what is burned on it, which is also what a saved picture of a printout is. */
  public void paint(Graphics2D canvas, int left, int top, int paperWidth, int paperHeight) {
    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        filtered ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
            : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    if (!filtered) {
      canvas.setColor(Color.WHITE);
      canvas.fillRect(left, top, paperWidth, paperHeight);
      if (strip == null && paper.height() > 0) {
        strip = plainDots();
        burnedAt = 1;
      }
      if (strip != null) {
        canvas.drawImage(strip, left, top, paperWidth,
            (int) Math.round(paper.height() * (paperWidth / (double) Printout.WIDTH)), null);
      }
      canvas.setColor(EDGE);
      canvas.drawRect(left, top, paperWidth, paperHeight);
      return;
    }

    roll(canvas, left, top, paperWidth, paperHeight);
    int resolution = burnResolution(paperWidth);
    if (strip == null || burnedAt != resolution) {
      strip = paper.height() == 0 ? null : burnedDots(resolution);
      burnedAt = resolution;
    }
    if (strip != null) {
      canvas.drawImage(strip, left, top, paperWidth,
          (int) Math.round(paper.height() * (paperWidth / (double) Printout.WIDTH)), null);
    }
    tornEdge(canvas, left, top, paperWidth);
  }

  /**
   * How finely to burn. Not the zoom: the picture is drawn once and scaled, so this only has to be
   * fine enough that scaling up does not show it, and coarse enough that a long printout is not a
   * hundred megabytes. Zoomed out it stays at three, which is what keeps a page seen small looking
   * like a page and not like a grid of pixels.
   */
  private int burnResolution(int paperWidth) {
    double perDot = paperWidth / (double) Printout.WIDTH;
    return Math.max(3, Math.min(8, (int) Math.ceil(perDot)));
  }

  /**
   * The paper: aluminium on a roll, so it is brighter down the middle where the light catches it
   * and it keeps the grain of having been rolled.
   */
  private void roll(Graphics2D canvas, int left, int top, int paperWidth, int paperHeight) {
    canvas.setPaint(new LinearGradientPaint(left, 0, left + paperWidth, 0,
        new float[]{0f, 0.35f, 0.62f, 1f},
        new Color[]{SILVER_EDGE, SILVER_MID, SILVER_MID, SILVER_EDGE}));
    canvas.fillRect(left, top, paperWidth, paperHeight);

    int grain = Math.max(6, (int) (paperWidth / 26.0));
    canvas.setColor(new Color(0xff, 0xff, 0xff, 12));
    for (int x = grain / 2; x < paperWidth; x += grain) {
      canvas.drawLine(left + x, top, left + x, top + paperHeight);
    }
    canvas.setColor(new Color(0x00, 0x00, 0x00, 8));
    for (int x = grain; x < paperWidth; x += (int) (grain * 1.7)) {
      canvas.drawLine(left + x, top, left + x, top + paperHeight);
    }

    canvas.setColor(EDGE);
    canvas.drawLine(left - 1, top, left - 1, top + paperHeight);
    canvas.drawLine(left + paperWidth, top, left + paperWidth, top + paperHeight);
  }

  /** The ragged line the paper is torn off at, along the top of the strip. */
  private void tornEdge(Graphics2D canvas, int left, int top, int paperWidth) {
    int tooth = Math.max(4, paperWidth / 42);
    canvas.setColor(EDGE);
    for (int x = 0; x < paperWidth; x += tooth) {
      int rise = x / tooth % 2 == 0 ? tooth / 2 + 1 : 1;
      canvas.drawLine(left + x, top - rise, left + Math.min(x + tooth, paperWidth),
          top - (tooth / 2 + 2 - rise));
    }
  }

  /**
   * The dots, drawn as burns rather than as squares: a spark eats a hole through the aluminium, it
   * spreads a little past where it struck, and no two are quite the same darkness - which is why a
   * printout from one of these looks like soot and not like a bitmap. Neighbours overlap, so a run
   * of dots comes out as one ragged stroke, which is what the eye recognises.
   */
  private BufferedImage burnedDots(int resolution) {
    BufferedImage dots = new BufferedImage(Printout.WIDTH * resolution,
        Math.max(paper.height() * resolution, 1), BufferedImage.TYPE_INT_ARGB);
    Graphics2D burn = dots.createGraphics();
    burn.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Wider than tall: the stylus is moving across the paper while it burns, so a dot is a short
    // smear along the line and not a circle.
    double smearX = resolution * 0.72;
    double smearY = resolution * 0.55;
    double halo = resolution * 0.95;
    for (int row = 0; row < paper.height(); row++) {
      boolean[] line = paper.row(row);
      // The belt does not come round to the same place every time, so a line sits a fraction of a
      // dot off from the one above it. Without this the print is on a perfect grid and reads as a
      // bitmap however the dots themselves are drawn.
      double wobble = (shade(row, -1) - 32) / 64.0 * resolution * 0.35;
      for (int dot = 0; dot < line.length; dot++) {
        if (!line[dot]) {
          continue;
        }
        double x = dot * resolution + resolution / 2.0 + wobble;
        double y = row * resolution + resolution / 2.0;
        int strength = shade(row, dot);

        burn.setColor(new Color(BURN.getRed(), BURN.getGreen(), BURN.getBlue(), 30 + strength / 4));
        burn.fill(new Ellipse2D.Double(x - halo, y - halo * 0.8, halo * 2, halo * 1.6));

        int lighter = (strength * 5) / 4;
        burn.setColor(new Color(
            Math.min(255, BURN.getRed() + lighter),
            Math.min(255, BURN.getGreen() + lighter),
            Math.min(255, BURN.getBlue() + lighter), 200 + strength / 2));
        burn.fill(new Ellipse2D.Double(x - smearX, y - smearY, smearX * 2, smearY * 2));
      }
    }
    burn.dispose();
    return dots;
  }

  /** The dots as the printer sent them, one pixel each: no paper, no burning, nothing added. */
  private BufferedImage plainDots() {
    BufferedImage dots = new BufferedImage(Printout.WIDTH, paper.height(), BufferedImage.TYPE_INT_ARGB);
    for (int row = 0; row < paper.height(); row++) {
      boolean[] line = paper.row(row);
      for (int dot = 0; dot < line.length; dot++) {
        if (line[dot]) {
          dots.setRGB(dot, row, 0xff000000);
        }
      }
    }
    return dots;
  }

  /**
   * How dark this particular dot came out. Hashed from where it is rather than drawn at random,
   * so that the same printout looks the same every time it is painted instead of crawling.
   */
  private int shade(int row, int dot) {
    int hash = row * 73856093 ^ (dot + 1) * 19349663;
    hash ^= hash >>> 13;
    return Math.abs(hash) % 64;
  }
}

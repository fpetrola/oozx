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
 * Renders a ZX Printer's roll as burned aluminium paper. The dot image is rasterised once at a
 * fixed resolution and then scaled for display, so zoom is a cheap blit rather than a redraw.
 */
public class PrinterPaper extends JPanel {
  /** Roll colours: brighter where the light catches the middle, darker at the edges. */
  private static final Color SILVER_EDGE = new Color(0xb8, 0xbb, 0xb3);
  private static final Color SILVER_MID = new Color(0xdc, 0xde, 0xd7);
  private static final Color BURN = new Color(0x1c, 0x1a, 0x17);
  private static final Color EDGE = new Color(0x94, 0x97, 0x8f);

  private static final double MIN_ZOOM = 0.4;
  private static final double MAX_ZOOM = 16;
  private static final int MARGIN = 14;

  private Printout paper;
  private final Printout.Listener watching = row -> SwingUtilities.invokeLater(this::rowArrived);

  /** Display scale, a fractional pixels-per-dot factor (paper is fixed at 256 dots wide). */
  private double zoom = 3;

  private BufferedImage strip;
  /** Resolution the cached bitmap was rasterised at; changes in coarser steps than the zoom. */
  private int burnedAt;

  private Point draggingFrom;

  /** False shows raw dots with no paper texture, for tests that check the output bitmap. */
  private boolean filtered = true;

  /**
   * Auto-scroll state, toggled by the scroll listener rather than checked when a row arrives:
   * checking only then would latch off permanently the first time a row lands a few pixels early.
   */
  private boolean following = true;
  private boolean scrollingItself;

  public PrinterPaper(Printout paper) {
    setBackground(new Color(0x2e, 0x30, 0x2c));
    setPrintout(paper);
    setAutoscrolls(true);

    // Consuming the wheel event here would break plain scrolling, so only ctrl+wheel is
    // handled and everything else is forwarded to the enclosing scroll pane.
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

  /** Switches to a different roll (or a blank one) and starts listening for its new rows. */
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

  /** Toggles between the textured paper render and a flat black-on-white dot dump. */
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

  /** Sets the zoom so the paper's full width fills the current viewport. */
  public void fitWidth() {
    int across = getVisibleRect().width;
    if (across > 2 * MARGIN) {
      setZoom((across - 2.0 * MARGIN) / Printout.WIDTH);
    }
  }

  /** Rescales around the pointer's paper coordinate so that point stays fixed on screen. */
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
    scrollingItself = true;
    try {
      revalidate();
      // The layout must run before the scroll below, or the request is clamped to the old,
      // shorter height.
      if (getParent() != null) {
        getParent().doLayout();
      }
      repaint();
      if (following) {
        scrollRectToVisible(new Rectangle(0, getPreferredSize().height - 1, 1, 1));
      }
    } finally {
      scrollingItself = false;
    }
  }

  /** True if the visible area reaches within a few rows of the bottom of the paper. */
  private boolean showingTheEnd() {
    Rectangle shown = getVisibleRect();
    return shown.y + shown.height >= getHeight() - Math.max(MARGIN, (int) (3 * zoom));
  }

  /** All scroll sources funnel through the viewport, so this is the single place to track
   * whether the view is still following the bottom of the roll. */
  @Override
  public void addNotify() {
    super.addNotify();
    if (getParent() instanceof javax.swing.JViewport viewport) {
      viewport.addChangeListener(moved -> {
        if (!scrollingItself) {
          following = showingTheEnd();
        }
      });
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

  /** Draws the roll and its dots; identical to what a saved snapshot of the printout captures. */
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
   * Bitmap pixels per dot, independent of zoom: clamped between 3 (avoids a pixelated look when
   * zoomed out) and 8 (keeps a long printout's cached image from ballooning in size).
   */
  private int burnResolution(int paperWidth) {
    double perDot = paperWidth / (double) Printout.WIDTH;
    return Math.max(3, Math.min(8, (int) Math.ceil(perDot)));
  }

  /** Paints the silver gradient and rolled-metal grain lines behind the burned dots. */
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

  /** Draws the jagged torn-paper edge above the roll. */
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
   * Renders each dot as an irregular burned spot rather than a square pixel, with randomised
   * darkness and overlap between neighbours so a run of dots reads as one sooty stroke.
   */
  private BufferedImage burnedDots(int resolution) {
    BufferedImage dots = new BufferedImage(Printout.WIDTH * resolution,
        Math.max(paper.height() * resolution, 1), BufferedImage.TYPE_INT_ARGB);
    Graphics2D burn = dots.createGraphics();
    burn.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // The stylus moves horizontally while burning, so each dot smears sideways, not round.
    double smearX = resolution * 0.72;
    double smearY = resolution * 0.55;
    double halo = resolution * 0.95;
    for (int row = 0; row < paper.height(); row++) {
      boolean[] line = paper.row(row);
      // Simulates belt slack: each line is offset a fraction of a dot so rows do not align
      // into a perfect, visibly synthetic grid.
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

  /** Unfiltered render: exactly the printer's bit pattern, one solid black pixel per set dot. */
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

  /** Deterministic per-dot darkness, derived from its position so repaints stay stable. */
  private int shade(int row, int dot) {
    int hash = row * 73856093 ^ (dot + 1) * 19349663;
    hash ^= hash >>> 13;
    return Math.abs(hash) % 64;
  }
}

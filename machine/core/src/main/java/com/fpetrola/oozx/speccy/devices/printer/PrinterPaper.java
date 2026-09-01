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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The paper coming out of a ZX Printer, drawn as the thing itself: a strip of aluminised paper
 * about four centimetres wide, with the dots burned through the silver to the black underneath.
 * <p>
 * It draws into an image the size of the printout and blits it, rather than painting dots one at a
 * time, because a full page is fifty thousand of them and this is asked to repaint on every row.
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

  private Printout paper;
  private final Printout.Listener watching = row -> SwingUtilities.invokeLater(this::rowArrived);
  private int scale = 3;
  private BufferedImage strip;

  public PrinterPaper(Printout paper) {
    setBackground(new Color(0x2e, 0x30, 0x2c));
    setPrintout(paper);
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

  /** How many screen pixels a dot is; the paper is 256 dots wide whatever this says. */
  public void setScale(int scale) {
    this.scale = Math.max(1, scale);
    strip = null;
    revalidate();
    repaint();
  }

  public int scale() {
    return scale;
  }

  public Printout printout() {
    return paper;
  }

  private void rowArrived() {
    strip = null;
    revalidate();
    repaint();
    scrollRectToVisible(new java.awt.Rectangle(0, getHeight() - 1, 1, 1));
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(Printout.WIDTH * scale + 28, Math.max(paper.height() * scale + 28, 140));
  }

  @Override
  protected void paintComponent(Graphics pen) {
    super.paintComponent(pen);
    Graphics2D canvas = (Graphics2D) pen.create();
    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int paperWidth = Printout.WIDTH * scale;
    int paperHeight = Math.max(paper.height() * scale, 80);
    int left = (getWidth() - paperWidth) / 2;

    paint(canvas, left, 14, paperWidth, paperHeight);
    canvas.dispose();
  }

  /** The strip and what is burned on it, which is also what a saved picture of a printout is. */
  public void paint(Graphics2D canvas, int left, int top, int paperWidth, int paperHeight) {
    roll(canvas, left, top, paperWidth, paperHeight);
    if (strip == null && paper.height() > 0) {
      strip = burnedDots();
    }
    if (strip != null) {
      canvas.drawImage(strip, left, top, paperWidth, paper.height() * scale, null);
    }
    tornEdge(canvas, left, top, paperWidth);
  }

  /**
   * The paper: aluminium on a roll, so it is brighter down the middle where the light catches it
   * and it keeps the grain of having been rolled.
   */
  private void roll(Graphics2D canvas, int left, int top, int paperWidth, int paperHeight) {
    canvas.setPaint(new java.awt.LinearGradientPaint(left, 0, left + paperWidth, 0,
        new float[]{0f, 0.35f, 0.62f, 1f},
        new Color[]{SILVER_EDGE, SILVER_MID, SILVER_MID, SILVER_EDGE}));
    canvas.fillRect(left, top, paperWidth, paperHeight);

    canvas.setColor(new Color(0xff, 0xff, 0xff, 12));
    for (int x = 5; x < paperWidth; x += 29) {
      canvas.drawLine(left + x, top, left + x, top + paperHeight);
    }
    canvas.setColor(new Color(0x00, 0x00, 0x00, 8));
    for (int x = 17; x < paperWidth; x += 41) {
      canvas.drawLine(left + x, top, left + x, top + paperHeight);
    }

    canvas.setColor(EDGE);
    canvas.drawLine(left - 1, top, left - 1, top + paperHeight);
    canvas.drawLine(left + paperWidth, top, left + paperWidth, top + paperHeight);
  }

  /** The ragged line the paper is torn off at, along the top of the strip. */
  private void tornEdge(Graphics2D canvas, int left, int top, int paperWidth) {
    canvas.setColor(EDGE);
    for (int x = 0; x < paperWidth; x += 6) {
      int rise = x / 6 % 2 == 0 ? 3 : 1;
      canvas.drawLine(left + x, top - rise, left + x + 6, top - (4 - rise));
    }
  }

  /**
   * The dots, drawn as burns rather than as squares: a spark eats a round hole through the
   * aluminium, it spreads a little past where it struck, and no two are quite the same darkness -
   * which is why a printout from one of these looks like soot and not like a bitmap. Neighbours
   * overlap, so a run of dots comes out as one ragged line, which is what the eye recognises.
   */
  private BufferedImage burnedDots() {
    int width = Printout.WIDTH * scale;
    int height = paper.height() * scale;
    BufferedImage dots = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    if (scale == 1) {
      for (int row = 0; row < paper.height(); row++) {
        boolean[] line = paper.row(row);
        for (int dot = 0; dot < line.length; dot++) {
          if (line[dot]) {
            dots.setRGB(dot, row, BURN.getRGB());
          }
        }
      }
      return dots;
    }

    Graphics2D burn = dots.createGraphics();
    burn.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Wider than tall: the stylus is moving across the paper while it burns, so a dot is a short
    // smear along the line and not a circle. Neighbours overlap and become one ragged stroke.
    double smearX = scale * 0.72;
    double smearY = scale * 0.55;
    double halo = scale * 0.95;
    for (int row = 0; row < paper.height(); row++) {
      boolean[] line = paper.row(row);
      // The belt does not come round to the same place every time, so a line sits a fraction of a
      // dot off from the one above it. Without this the print is on a perfect grid and reads as a
      // bitmap however the dots themselves are drawn.
      double wobble = (shade(row, -1) - 32) / 64.0 * scale * 0.35;
      for (int dot = 0; dot < line.length; dot++) {
        if (!line[dot]) {
          continue;
        }
        double x = dot * scale + scale / 2.0 + wobble;
        double y = row * scale + scale / 2.0;
        int strength = shade(row, dot);

        burn.setColor(new Color(BURN.getRed(), BURN.getGreen(), BURN.getBlue(), 30 + strength / 4));
        burn.fill(new java.awt.geom.Ellipse2D.Double(x - halo, y - halo * 0.8, halo * 2, halo * 1.6));

        int lighter = (strength * 5) / 4;
        burn.setColor(new Color(
            Math.min(255, BURN.getRed() + lighter),
            Math.min(255, BURN.getGreen() + lighter),
            Math.min(255, BURN.getBlue() + lighter), 200 + strength / 2));
        burn.fill(new java.awt.geom.Ellipse2D.Double(x - smearX, y - smearY, smearX * 2, smearY * 2));
      }
    }
    burn.dispose();
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

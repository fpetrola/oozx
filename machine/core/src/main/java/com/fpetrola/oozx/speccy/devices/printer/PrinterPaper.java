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
  private static final Color SILVER_TOP = new Color(0xd7, 0xd9, 0xd2);
  private static final Color SILVER_BOTTOM = new Color(0xc2, 0xc5, 0xbd);
  private static final Color BURN = new Color(0x22, 0x20, 0x1c);
  private static final Color EDGE = new Color(0x9a, 0x9d, 0x95);

  private Printout paper;
  private final Printout.Listener watching = row -> SwingUtilities.invokeLater(this::rowArrived);
  private int scale = 2;
  private BufferedImage strip;

  public PrinterPaper(Printout paper) {
    setBackground(new Color(0x3a, 0x3c, 0x38));
    setPreferredSize(new Dimension(Printout.WIDTH * scale + 24, 220));
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
    return new Dimension(Printout.WIDTH * scale + 24, Math.max(paper.height() * scale + 24, 120));
  }

  @Override
  protected void paintComponent(Graphics pen) {
    super.paintComponent(pen);
    Graphics2D canvas = (Graphics2D) pen.create();
    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int paperWidth = Printout.WIDTH * scale;
    int paperHeight = Math.max(paper.height() * scale, 60);
    int left = (getWidth() - paperWidth) / 2;

    canvas.setPaint(new GradientPaint(left, 0, SILVER_TOP, left + paperWidth, 0, SILVER_BOTTOM));
    canvas.fillRect(left, 12, paperWidth, paperHeight);
    canvas.setColor(EDGE);
    canvas.drawLine(left - 1, 12, left - 1, 12 + paperHeight);
    canvas.drawLine(left + paperWidth, 12, left + paperWidth, 12 + paperHeight);
    tornEdge(canvas, left, paperWidth);

    if (strip == null && paper.height() > 0) {
      strip = burnedDots();
    }
    if (strip != null) {
      canvas.drawImage(strip, left, 12, paperWidth, paper.height() * scale, null);
    }
    canvas.dispose();
  }

  /** The ragged line the paper is torn off at, along the top of the strip. */
  private void tornEdge(Graphics2D canvas, int left, int paperWidth) {
    canvas.setColor(EDGE);
    for (int x = 0; x < paperWidth; x += 6) {
      canvas.drawLine(left + x, 12 - (x / 6 % 2 == 0 ? 3 : 1), left + x + 6, 12 - (x / 6 % 2 == 0 ? 1 : 3));
    }
  }

  private BufferedImage burnedDots() {
    BufferedImage dots = new BufferedImage(Printout.WIDTH, paper.height(), BufferedImage.TYPE_INT_ARGB);
    int burn = BURN.getRGB();
    for (int row = 0; row < paper.height(); row++) {
      boolean[] line = paper.row(row);
      for (int dot = 0; dot < line.length; dot++) {
        if (line[dot]) {
          dots.setRGB(dot, row, burn);
        }
      }
    }
    return dots;
  }
}

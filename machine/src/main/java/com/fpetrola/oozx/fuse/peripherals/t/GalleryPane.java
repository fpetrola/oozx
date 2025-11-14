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

package com.fpetrola.oozx.fuse.peripherals.t;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Gallery panel that only updates images when they become visible in the scrollable pane, which is the parent of this
 * panel.
 * @author Andi Hotz, (c) Sahits GmbH, 2012
 * Created on Jul 31, 2012
 *
 */
public class GalleryPane extends JPanel implements AdjustmentListener, ComponentListener {
  private static final int SPACEING = 5;
  private static final int IMAGE_DIM = 50;
  private final BufferedImage[] imgs;
  private final JLabel[] icons;
  /** Nubmer of rows that are visible in the viewport */
  private double visibleNbRows;

  /** Model for the vertical scroll bar */
  private BoundedRangeModel brm;

  private final int nbCols = 3;
  private final int nbRows;

  /**
   * Create the panel.
   */
  public GalleryPane() {
    imgs = new BufferedImage[30];
    icons = new JLabel[imgs.length];
    for (int i = 0; i < imgs.length; i++) {
      imgs[i] = null;
    }
    nbRows = (int) Math.ceil(1.0 * imgs.length / nbCols);
    setBorder(new EmptyBorder(5, 5, 5, 5));
    setLayout(new GridLayout(nbRows, nbCols + 1, SPACEING, SPACEING));
    BufferedImage blackSquare = new BufferedImage(IMAGE_DIM, IMAGE_DIM, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D g = (Graphics2D) blackSquare.getGraphics();
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, blackSquare.getWidth(), blackSquare.getHeight());
    Icon icon = new ImageIcon(blackSquare);
    for (int i = 0; i < imgs.length; i++) {
      if (i % 3 == 0) {
        add(new JLabel(String.valueOf(i / 3)));
      }
      icons[i] = new JLabel(icon);
      add(icons[i]);
    }
  }

  /**
   * This method is called when the scroll bar is moved.
   * @see AdjustmentListener#adjustmentValueChanged(AdjustmentEvent)
   */
  public void adjustmentValueChanged(AdjustmentEvent e) {
    updateView();
  }

  /**
   * Make sure that everything that is visible is loaded
   */
  private void updateView() {
    double percentage = 100.0 * brm.getValue() / (brm.getMaximum() - brm.getExtent());
    int firstRow = computeFirstRowIndex(percentage / 100);
    int lastRow = computeLastRowIndex(percentage / 100);
    updateImages(firstRow, lastRow);
  }

  /**
   * Load and unload the images that are contained between the rows
   * @param firstRow
   * @param lastRow
   */
  private void updateImages(int firstRow, int lastRow) {
    int firtsImageIndex = firstRow * nbCols;
    int lastImageIndex = Math.min(lastRow * nbCols + nbCols - 1, imgs.length - 1); // make sure we do not try accessing an element that is not there
    for (int i = firtsImageIndex; i <= lastImageIndex; i++) {
      if (imgs[i] == null) {
        BufferedImage blackSquare = new BufferedImage(IMAGE_DIM, IMAGE_DIM, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = (Graphics2D) blackSquare.getGraphics();
        BufferedImage img = new BufferedImage(IMAGE_DIM, IMAGE_DIM, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        FontRenderContext frc = g2d.getFontRenderContext();

        Font font = g2d.getFont();
        font = font.deriveFont(24);

        GlyphVector gv = font.createGlyphVector(frc, String.valueOf(i));
        Rectangle2D rect = gv.getVisualBounds();
        int x = (int) ((IMAGE_DIM - rect.getWidth()) / 2);
        int y = (int) ((IMAGE_DIM - rect.getHeight()) / 2);
        g2d.drawGlyphVector(gv, x, y);
        g.drawImage(img, 0, 0, this);
        icons[i].setIcon(new ImageIcon(blackSquare));
      }
    }
  }

  /**
   * Figure out the index of the last row to be shown
   * @param percentage of scrolling [0,1]
   * @return
   */
  private int computeLastRowIndex(double percentage) {
    double pos = computeTopPosition(percentage);
    int result = (int) Math.floor((pos - SPACEING) / (SPACEING + IMAGE_DIM) + visibleNbRows);
    return Math.min(result, nbRows);
  }

  /**
   * Figure out the index of the first row to be shown
   * @param percentage of scrolling [0,1]
   * @return
   */
  private int computeFirstRowIndex(double percentage) {
    double pos = computeTopPosition(percentage);
    int result = (int) Math.floor((pos - SPACEING) / (SPACEING + IMAGE_DIM));
    return Math.max(0, result);
  }

  /**
   * Compute the top position in the panel that is visible.
   * @param percentage
   * @return
   */
  private double computeTopPosition(double percentage) {
    /*
     * The percentage indicates the scroll position, so 0% means all to
     * the top, 100% is all the bottom. The top position of the bottom however
     * is the height of the panel - the height of the viewport (parent). anything
     * in between is procentual
     */
    double top = getBounds().height * percentage;
    double correction = getParent().getBounds().height * percentage;
    return top - correction;
  }

  public void setBrm(BoundedRangeModel brm) {
    this.brm = brm;
  }

  /**
   * We are not interested in reacting when the component becomes hidden
   * @see ComponentListener#componentHidden(ComponentEvent)
   */
  public void componentHidden(ComponentEvent e) {
  }

  /**
   * We are not interested in reacting when the component is moved
   * @see ComponentListener#componentMoved(ComponentEvent)
   */
  public void componentMoved(ComponentEvent e) {
  }

  /**
   * When the component is resized, the visible portion changes, take kare of the
   * appropriate actions.
   * @see ComponentListener#componentResized(ComponentEvent)
   */
  public void componentResized(ComponentEvent e) {
    int rowHeigth = SPACEING + IMAGE_DIM;
    visibleNbRows = 1.0 * getParent().getBounds().height / rowHeigth;
    updateView(); // make sure the view is properly updated
  }

  /**
   * We are not interested in reacting when the component is shown again.
   * @see ComponentListener#componentShown(ComponentEvent)
   */
  public void componentShown(ComponentEvent e) {
  }

}

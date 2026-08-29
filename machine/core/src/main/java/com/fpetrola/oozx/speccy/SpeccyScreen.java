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

package com.fpetrola.oozx.speccy;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class SpeccyScreen extends JPanel {
  private final byte[][] screenMatrix;
  private final BufferedImage screenBuffer;
  private double zoom = 2;
  Color[] lightColors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
  Color[] darkColors = new Color[8];

  private final int width = 256 + 48 + 48 - 32;
  private final int height = 192 + 64 + 56 - 56 - 20;

  /**
   * Where the Speccy's own 256x192 sits inside the matrix, which is drawn border and all.
   * <p>
   * The display plots a chunk at {@code x + BORDER_WIDTH_COLS} columns of eight pixels and
   * {@code y + BORDER_HEIGHT} rows, so the picture starts four columns in and three rows down.
   */
  private static final int SCREEN_X = 4 * 8;
  private static final int SCREEN_Y = 3 * 8;
  private static final int SCREEN_W = 256;
  private static final int SCREEN_H = 192;

  /**
   * Off to begin with: the border is what the machine had around the picture, not part of it,
   * and most of the time it is a colour going by. It is a button away when a game uses it for
   * something - the loading stripes, or a game that flashes it.
   */
  private volatile boolean borderVisible;
  private BufferedImage croppedBuffer;

  /**
   * Which lead the picture came down. Off to begin with, because a monitor is the honest default
   * and a television is a thing you ask for.
   */
  private volatile TvScreen tv = TvScreen.RGB_MONITOR;
  private final TvScreen.Scratch tvScratch = new TvScreen.Scratch();

  /** Dark lines between the bright ones, the way a television left the gaps unlit. */
  private volatile boolean scanLines;

  /**
   * Whether to smooth when scaling, or null to go on deciding it by the scale as this always
   * has: sharp when the picture lands on whole pixels, smoothed when it does not.
   */
  private volatile Boolean smoothing;

  public SpeccyScreen(byte[][] screenMatrix) {
    IntStream.range(0, 8).forEach(i -> darkColors[i] = lightColors[i].darker());
    this.screenMatrix = screenMatrix;
    setPreferredSize(new Dimension((int) (SCREEN_W * zoom), (int) (SCREEN_H * zoom)));
    this.screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.croppedBuffer = new BufferedImage(SCREEN_W, SCREEN_H, BufferedImage.TYPE_INT_RGB);

    new Timer(30, e -> SwingUtilities.invokeLater(this::repaint)).start();
  }

  /**
   * Shows or hides the border, and gives the window the room for it or takes the room back.
   * <p>
   * Cropping alone is not what turning the border off looks like: the panel keeps its size, the
   * picture is scaled up to fill it, and what you see is the same picture slightly bigger rather
   * than a border that went away. So the window changes by exactly what the border was taking on
   * screen, and the picture stays the size it was - the border appears around it and leaves from
   * around it.
   * <p>
   * The scale is measured from the panel as it stands rather than assumed, because the window is
   * whatever size it was last dragged to, and the paint below fits the picture into it the same
   * way: the smaller of the two ratios.
   */
  public void setBorderVisible(boolean borderVisible) {
    if (this.borderVisible == borderVisible) {
      return;
    }
    int previousWidth = imageWidth();
    int previousHeight = imageHeight();
    this.borderVisible = borderVisible;
    setPreferredSize(new Dimension((int) (imageWidth() * zoom), (int) (imageHeight() * zoom)));
    resizeWindowBy(previousWidth, previousHeight);
    repaint();
  }

  private void resizeWindowBy(int previousWidth, int previousHeight) {
    Container window = SwingUtilities.getAncestorOfClass(JInternalFrame.class, this);
    if (window == null) {
      window = SwingUtilities.getWindowAncestor(this);
    }
    if (window == null || getWidth() == 0 || getHeight() == 0) {
      revalidate();
      return;
    }
    double scale = Math.min(getWidth() / (double) previousWidth, getHeight() / (double) previousHeight);
    int growWidth = (int) Math.round((imageWidth() - previousWidth) * scale);
    int growHeight = (int) Math.round((imageHeight() - previousHeight) * scale);
    window.setSize(window.getWidth() + growWidth, window.getHeight() + growHeight);
    window.validate();
  }

  /** Which lead the picture comes down; {@link TvScreen#RGB_MONITOR} is none at all. */
  public void setTvScreen(TvScreen tv) {
    this.tv = tv == null ? TvScreen.RGB_MONITOR : tv;
    repaint();
  }

  public TvScreen getTvScreen() {
    return tv;
  }

  public void setScanLines(boolean scanLines) {
    this.scanLines = scanLines;
    repaint();
  }

  public boolean isScanLines() {
    return scanLines;
  }

  /** True to smooth, false to keep it blocky, null to go on deciding by the scale. */
  public void setSmoothing(Boolean smoothing) {
    this.smoothing = smoothing;
    repaint();
  }

  public Boolean getSmoothing() {
    return smoothing;
  }

  /**
   * The unlit gaps a television left between its lines.
   * <p>
   * Drawn on the scaled picture rather than the small one, because that is where they belong: a
   * scan line is a line of the SCREEN, and one drawn before scaling would be stretched into a
   * dark band as wide as the magnification. Which also says when they are worth drawing at all -
   * below two screen pixels to a machine pixel there is no gap to leave, and darkening every
   * other row would only halve the brightness.
   */
  private static void darkenAlternateLines(BufferedImage scaled, int sourceHeight) {
    if (scaled.getHeight() < sourceHeight * 2) {
      return;
    }
    int width = scaled.getWidth();
    int[] row = new int[width];
    for (int y = 1; y < scaled.getHeight(); y += 2) {
      scaled.getRGB(0, y, width, 1, row, 0, width);
      for (int x = 0; x < width; x++) {
        int rgb = row[x];
        row[x] = rgb & 0xFF000000
            | (((rgb >> 16) & 0xFF) * 55 / 100) << 16
            | (((rgb >> 8) & 0xFF) * 55 / 100) << 8
            | ((rgb & 0xFF) * 55 / 100);
      }
      scaled.setRGB(0, y, width, 1, row, 0, width);
    }
  }

  private int imageWidth() {
    return borderVisible ? width : SCREEN_W;
  }

  private int imageHeight() {
    return borderVisible ? height : SCREEN_H;
  }

  public boolean isBorderVisible() {
    return borderVisible;
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    boolean withBorder = borderVisible;
    BufferedImage target = withBorder ? screenBuffer : croppedBuffer;
    int originX = withBorder ? 0 : SCREEN_X;
    int originY = withBorder ? 0 : SCREEN_Y;
    for (int x = 0; x < target.getWidth(); x++) {
      for (int y = 0; y < target.getHeight(); y++) {
        int zxColorCode = screenMatrix[originX + x][originY + y];
        target.setRGB(x, y, (zxColorCode >= 8 ? lightColors[zxColorCode - 8] : darkColors[zxColorCode]).getRGB());
      }
    }
    // Here and not after scaling: what a lead does to a picture, it does to the machine's own
    // pixels, and a blur measured in the pixels of a window someone resized measures the window.
    tv.apply(target, tvScratch);

//    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);

    BufferedImage image = target;

    if (image != null) {

      int imgWidth, imgHeight;
      double contRatio = (double) getWidth() / (double) getHeight();
      double imgRatio = (double) image.getWidth(this) / (double) image.getHeight(this);

      //width limited
      if (contRatio < imgRatio) {
        imgWidth = getWidth();
        imgHeight = (int) (getWidth() / imgRatio);
        //height limited
      } else {
        imgWidth = (int) (getHeight() * imgRatio);
        imgHeight = getHeight();
      }

      double i = (imgWidth * 1000f / image.getWidth(this) * 1000f) / 10000f;
      boolean b = i % 100f < 30f;
//      System.out.println(i + " -> " + b);
      double ceil = Math.ceil(i / 100f) - 1;
      if (b && ceil > 0) {
        imgWidth = (int) (ceil * image.getWidth(this));
        imgHeight = (int) (ceil * image.getHeight(this));
      }
      // Sharp or smoothed: by the scale unless someone has said which they want.
      Boolean asked = smoothing;
      BufferedImage scaledImage = getScaledImage(image, imgWidth, imgHeight, asked == null ? b : !asked);
      if (scanLines) {
        darkenAlternateLines(scaledImage, image.getHeight());
      }
      //to center
      int x = (int) (((double) getWidth() / 2) - ((double) imgWidth / 2));
      int y = (int) (((double) getHeight() / 2) - ((double) imgHeight / 2));
      g.drawImage(scaledImage, x, y, this);
    }

  }

  public static BufferedImage getScaledImage(BufferedImage image, int width, int height, boolean b) {
    try {
      int imageWidth = image.getWidth();
      int imageHeight = image.getHeight();

      double scaleX = (double) width / imageWidth;
      double scaleY = (double) height / imageHeight;
      AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
      AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, b ? AffineTransformOp.TYPE_NEAREST_NEIGHBOR : AffineTransformOp.TYPE_BILINEAR);

      return bilinearScaleOp.filter(image, new BufferedImage(width, height, image.getType()));
    } catch (Exception e) {
      return image;
    }
  }
}

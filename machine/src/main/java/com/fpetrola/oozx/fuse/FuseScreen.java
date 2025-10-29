/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class FuseScreen extends JPanel {
  private final byte[][] screenMatrix;
  private final BufferedImage screenBuffer;
  private double zoom = 2;
  Color[] lightColors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
  Color[] darkColors = new Color[8];

  private int width = 256 + 48 + 48 - 32;
  private int height = 192 + 64 + 56 - 56 - 20;

  public FuseScreen(byte[][] screenMatrix) {
    IntStream.range(0, 8).forEach(i -> darkColors[i] = lightColors[i].darker());
    this.screenMatrix = screenMatrix;
    setPreferredSize(new Dimension((int) (width * zoom), (int) (height * zoom)));
    this.screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    new Timer(30, e -> {
      repaint();
    }).start();
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int zxColorCode = screenMatrix[x][y];
        screenBuffer.setRGB(x, y, (zxColorCode >= 8 ? lightColors[zxColorCode - 8] : darkColors[zxColorCode]).getRGB());
      }
    }

//    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);

    BufferedImage image = screenBuffer;

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
        BufferedImage scaledImage = getScaledImage(image, imgWidth, imgHeight, b);
      //to center
      int x = (int) (((double) getWidth() / 2) - ((double) imgWidth / 2));
      int y = (int) (((double) getHeight() / 2) - ((double) imgHeight / 2));
      g.drawImage(scaledImage, x, y, this);
    }

  }

  public static BufferedImage getScaledImage(BufferedImage image, int width, int height, boolean b) {
    int imageWidth = image.getWidth();
    int imageHeight = image.getHeight();

    double scaleX = (double) width / imageWidth;
    double scaleY = (double) height / imageHeight;
    AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
    AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, b ? AffineTransformOp.TYPE_NEAREST_NEIGHBOR : AffineTransformOp.TYPE_BILINEAR);

    return bilinearScaleOp.filter(image, new BufferedImage(width, height, image.getType()));
  }
}

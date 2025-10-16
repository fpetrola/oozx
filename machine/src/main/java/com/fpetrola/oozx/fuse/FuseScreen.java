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
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class FuseScreen extends JPanel {
  private final byte[][] screenMatrix;
  private final BufferedImage screenBuffer;
  private double zoom = 2;
  Color[] lightColors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
  Color[] darkColors = new Color[8];

  private int width = 256 + 48 + 48- 32;
  private int height = 192 + 64 + 56 - 56;

  public FuseScreen(byte[][] screenMatrix) {
    IntStream.range(0, 8).forEach(i -> darkColors[i] = lightColors[i].darker());
    this.screenMatrix = screenMatrix;
    setPreferredSize(new Dimension((int) (width * zoom), (int) (height * zoom)));
    this.screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    new Timer(10, e -> repaint()).start();
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int zxColorCode = screenMatrix[x][y];
        screenBuffer.setRGB(x, y, (zxColorCode >= 8 ? lightColors[zxColorCode - 8] : darkColors[zxColorCode]).getRGB());
      }
    }

    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
  }
}

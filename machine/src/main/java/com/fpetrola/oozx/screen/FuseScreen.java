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

package com.fpetrola.oozx.screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.Function;

public class FuseScreen extends JPanel {
  protected final Function<Integer, Integer> screenMemory;
  private final byte[][] screenMatrix;
  private final BufferedImage screenBuffer;
  private double zoom = 2;
  Color[] colors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
  private int width = 256 + 48 + 48;
  private int height = 192 + 64 + 56;

  public FuseScreen(Function<Integer, Integer> screenMemory, byte[][] screenMatrix) {
    this.screenMemory = screenMemory;
    this.screenMatrix = screenMatrix;
    setPreferredSize(new Dimension((int) (256 + 100 * zoom), (int) (192 + 100 * zoom)));
    this.screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    new Timer(20, e -> {
      repaint();
    }).start();

//    this.addComponentListener(new ComponentAdapter() {
//      public void componentResized(ComponentEvent e) {
//        zoom = (e.getComponent().getSize().getWidth() / (128));
//      }
//    });
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        byte screenMatrix1 = screenMatrix[x][y];
        int zxColorCode = screenMatrix1;

        Color c = zxColorCode >= 8 ? colors[zxColorCode - 8] : colors[zxColorCode].darker();
        screenBuffer.setRGB(x, y, c.getRGB());
      }
    }

    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
  }
}

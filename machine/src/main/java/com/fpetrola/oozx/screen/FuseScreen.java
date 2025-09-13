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
import java.util.function.Function;

public class FuseScreen extends JPanel {
  protected final Function<Integer, Integer> screenMemory;
  private final byte[][] screenMatrix;
  private double zoom = 2;
  Color[] colors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};

  public FuseScreen(Function<Integer, Integer> screenMemory, byte[][] screenMatrix) {
    this.screenMemory = screenMemory;
    this.screenMatrix = screenMatrix;
    setPreferredSize(new Dimension((int) (256+100 * zoom), (int) (192+100 * zoom)));

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

    Graphics2D g2d = (Graphics2D) g.create();
    AffineTransform at = new AffineTransform();
    at.scale(zoom, zoom);
    g2d.setTransform(at);

    for (int x = 0; x < 256+48+48; x++) {
      for (int y = 0; y < 192+64+56; y++) {
        byte screenMatrix1 = screenMatrix[x][y];
        int zxColorCode = screenMatrix1;
        g2d.setColor(zxColorCode >= 8 ? colors[zxColorCode - 8] : colors[zxColorCode].darker());
        g2d.fillRect(x, y, 1, 1);
      }
    }

//    for (int i = 0; i < newScreen.length; i++) {
//      double x = i % 256;
//      double y = i / 256;
//
//      int zxColorCode = newScreen[i];
//      g2d.setColor(zxColorCode >= 8 ? colors[zxColorCode - 8] : colors[zxColorCode].darker());
//      g2d.fillRect((int) x, (int) y, 1, 1);
//    }
    g2d.dispose();
  }
}

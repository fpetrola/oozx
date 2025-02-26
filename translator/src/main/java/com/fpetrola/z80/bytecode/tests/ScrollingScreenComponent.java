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

package com.fpetrola.z80.bytecode.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.List;

public class ScrollingScreenComponent extends JComponent {
  private final List<GameTile> zxScreenComponent;
  private final GameTile mainGameTile;
  private int t;

  private DoublePoint currentTranslate;

  private int mx;
  private int my;

  public ScrollingScreenComponent(List<GameTile> zxScreenComponent, GameTile mainGameTile) {
    this.zxScreenComponent = zxScreenComponent;
    this.mainGameTile = mainGameTile;
    setPreferredSize(new Dimension(256 * 4, 192 * 3));

    new Timer(10, e -> {
      repaint();
    }).start();
  }

  protected void paintComponent(Graphics g) {
    int xTiles = 9;
    int yTiles = 4;

    double scale = 2.0;
    int tileWidth = 256;
    int tileHeight = 128;

    super.paintComponent(g);
    BufferedImage combined = new BufferedImage((xTiles + 3) * tileWidth, (yTiles + 4) * tileHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2b = (Graphics2D) combined.getGraphics();

    for (int i = 0; i < zxScreenComponent.size(); i++) {
      GameTile gameTile = zxScreenComponent.get(i);

      if (gameTile.x == mainGameTile.x && gameTile.y == mainGameTile.y) {
        gameTile = mainGameTile;
      }
      BufferedImage screenBuffer1 = gameTile.zxScreenComponent.screenBuffer;
      screenBuffer1 = screenBuffer1.getSubimage(0, 0, tileWidth, tileHeight);
      BufferedImage after = screenBuffer1;
      int x = gameTile.x * tileWidth;
      int y = gameTile.y * tileHeight;
      g2b.drawImage(after, x, y, after.getWidth(), after.getHeight(), null);
//      g2b.drawString("X:" + gameTile.x + " Y:" + gameTile.y, x, y);
    }
    g2b.dispose();

    DoublePoint o = new DoublePoint(-((mainGameTile.x - 1) * tileWidth * scale), -((mainGameTile.y - 2) * tileHeight * scale));

    if (currentTranslate == null)
      currentTranslate = o;

    if (Math.abs(currentTranslate.x - o.x) > 10) {
      double v = (currentTranslate.x - o.x) / 30f;
      currentTranslate.x -= v;
    }
    if (Math.abs(currentTranslate.y - o.y) > 10) {
      double v = (currentTranslate.y - o.y) / 30f;
      currentTranslate.y -= v;
    }

    AffineTransform tx = new AffineTransform();
    tx.translate(currentTranslate.x, currentTranslate.y);
    tx.scale(scale, scale);
//
//
//    AffineTransformOp scaleOp = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//    BufferedImage after = new BufferedImage((int) (combined.getWidth() * scale), (int) (combined.getHeight() * scale), BufferedImage.TYPE_INT_ARGB);
//    Graphics2D g2 = (Graphics2D) after.getGraphics();
//    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
////    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
//    g2.drawImage(combined, scaleOp, 0, 0);
//    g2.dispose();


//    AffineTransformOp scaleOp2 = new AffineTransformOp(AffineTransform.getTranslateInstance(currentTranslate.x, currentTranslate.y), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    BufferedImage combined2 = new BufferedImage(combined.getWidth(), combined.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2b3 = (Graphics2D) combined2.getGraphics();
//    g2b3.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//    g2b3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g2b3.setTransform(tx);
    g2b3.drawImage(combined, 0, 0, null);
    g2b3.dispose();

    g.setColor(Color.BLACK);
    g.fillRect(0, 0, combined2.getWidth(), combined2.getHeight());
    g.drawImage(combined2, 0, 0, combined2.getWidth(), combined2.getHeight(), null);

  }

}

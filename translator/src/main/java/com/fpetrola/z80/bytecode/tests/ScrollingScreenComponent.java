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

import org.imgscalr.Scalr;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.List;

public class ScrollingScreenComponent extends JComponent {
  private final List<GameTile> zxScreenComponent;
  private final GameTile mainGameTile;

  private DoublePoint currentTranslate;
  public double scale;
  private int combinedWidth;
  private int combinedHeight;
  private double lastScale;

  public ScrollingScreenComponent(List<GameTile> zxScreenComponent, GameTile mainGameTile) {
    this.zxScreenComponent = zxScreenComponent;
    this.mainGameTile = mainGameTile;
    setPreferredSize(new Dimension(256 * 4, 192 * 3));
    scale = 1;

    new Timer(1, e -> repaint()).start();
  }

  protected void paintComponent(Graphics g) {
    int xTiles = 18;
    int yTiles = 7;

    double tileWidth = 256;
    double tileHeight = 128;

    BufferedImage combined = getCombinedImage(xTiles, (int) tileWidth, yTiles, (int) tileHeight);

    int combined2Width1 = combined.getWidth();
    int combined2Height2 = combined.getHeight();

    g.setColor(Color.RED);
    g.fillRect(0, 0, getWidth(), getHeight());
    DoublePoint translate = updateTranslate((int) tileWidth, (int) tileHeight);

    boolean crop = true;
    if (crop) {
      double x = translate.x;
      double y = translate.y;

      double width = tileWidth / 2 / scale + getMiddleWidth() / scale;
      double height = tileHeight / 2 / scale + getMiddleHeight() / scale;

      if (width > combined2Width1 - x) {
        width = combined2Width1 - x;
      }

      if (height > combined2Height2 - y) {
        height = combined2Height2 - y;
      }

      AffineTransformOp affineTransformOp = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
      BufferedImage cropped = Scalr.crop(combined, (int) x, (int) y, (int) width, (int) height, affineTransformOp);
      int x1 = 0;

//      BufferedImage scaledImage = Scalr.resize(cropped, Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, getWidth(), getHeight());
//      g.drawImage(scaledImage, 0, 0, Color.BLACK, null);

      g.drawImage(cropped, x1, x1, getWidth() - x1, getHeight() - x1, Color.BLACK, null);
    } else {
      BufferedImage scaledImage = Scalr.resize(combined, Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, (int) (combined.getWidth() * scale), (int) (combined.getHeight() * scale));
      g.drawImage(scaledImage, (int) (translate.x), (int) (translate.y), Color.BLACK, null);
    }

//    g.drawString("scale: %s".formatted(scale), 100, 100);

    //    g.drawString("tx: " + translation.x + " ty:" + translation.y, 100, 100);
  }

  private BufferedImage getCombinedImage(int xTiles, int tileWidth, int yTiles, int tileHeight) {
    combinedWidth = xTiles * tileWidth;
    combinedHeight = yTiles * tileHeight;
    BufferedImage combined = new BufferedImage(combinedWidth, combinedHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2b = (Graphics2D) combined.getGraphics();

    for (int i = 0; i < zxScreenComponent.size(); i++) {
      GameTile gameTile = zxScreenComponent.get(i);


      if (gameTile.x == mainGameTile.x && gameTile.y == mainGameTile.y) {
        gameTile = mainGameTile;
      }

      boolean isVisible = checkPosition(gameTile, 0, 0)
          || checkPosition(gameTile, 1, 0)
          || checkPosition(gameTile, -1, 0)
          || checkPosition(gameTile, 0, 1)
          || checkPosition(gameTile, 0, -1);
      gameTile.tileSpec.visible = isVisible;
      gameTile.tileSpec.visible = true;

      BufferedImage screenBuffer1 = gameTile.zxScreenComponent.screenBuffer;
      screenBuffer1 = screenBuffer1.getSubimage(0, 0, tileWidth, tileHeight);
      BufferedImage after = screenBuffer1;
      int x = gameTile.x * tileWidth;
      int y = gameTile.y * tileHeight;
      g2b.drawImage(after, x, y, after.getWidth(), after.getHeight(), null);
//      g2b.drawString("X:" + gameTile.x + " Y:" + gameTile.y, x, y);
    }
    g2b.dispose();

    BufferedImage combined2 = new BufferedImage(combined.getWidth() * 3, combined.getHeight() * 3, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2b2 = (Graphics2D) combined2.getGraphics();

    g2b2.drawImage(combined, combined.getWidth(), combined.getHeight(), null);
    g2b2.dispose();

    return combined2;
  }

  private boolean checkPosition(GameTile gameTile, int dx, int dy) {
    return gameTile.x + dx == mainGameTile.x && gameTile.y + dy == mainGameTile.y;
  }

  private DoublePoint updateTranslate(int tileWidth, int tileHeight) {
    DoublePoint o = getTranslation(tileWidth, tileHeight);
    if (o.x < 0 || o.y < 0) {
      o = currentTranslate;
      scale = lastScale;
    }
    if (currentTranslate == null)
      currentTranslate = o;

    currentTranslate.x = move(currentTranslate.x, o.x);
    currentTranslate.y = move(currentTranslate.y, o.y);
    lastScale = scale;
    return currentTranslate;
  }

  private double move(double currentCoordinate, double newCoordinate) {
    double coordinate;
    double abs1 = Math.abs(currentCoordinate - newCoordinate);
    double v = ((currentCoordinate - newCoordinate) / 70 + 1);
    double abs = Math.abs(v);
    v = v * abs / 2;
    coordinate = currentCoordinate - v;

    return coordinate;
  }

  private DoublePoint getTranslation(int tileWidth, int tileHeight) {
    //    middleWidth= middleHeight= 0;
    double tx = -((mainGameTile.x) * tileWidth * scale) - ((double) tileWidth / 2 * scale) + getMiddleWidth();
    double ty = -((mainGameTile.y) * tileHeight * scale) - ((double) tileHeight / 2 * scale) + getMiddleHeight();
//    tx= ty= 0;

    double x = (mainGameTile.x * tileWidth + tileWidth / 2) / 1 - getMiddleWidth() / 2 / scale + combinedWidth;
    double y = (mainGameTile.y * tileHeight + tileHeight / 2) / 1 - getMiddleHeight() / 2 / scale + combinedHeight;
    return new DoublePoint(x, y);
  }

  private double getMiddleHeight() {
    return (double) getHeight() / 2;
  }

  private double getMiddleWidth() {
    return (double) getWidth() / 2;
  }

}

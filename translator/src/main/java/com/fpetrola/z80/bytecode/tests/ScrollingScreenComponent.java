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
import java.awt.image.BufferedImage;
import java.util.List;

public class ScrollingScreenComponent extends JComponent {
  private final List<GameTile> zxScreenComponent;
  private final GameTile mainGameTile;

  public ScrollingScreenComponent(List<GameTile> zxScreenComponent, GameTile mainGameTile) {
    this.zxScreenComponent = zxScreenComponent;
    this.mainGameTile = mainGameTile;
    setPreferredSize(new Dimension(256 * 8, 192 * 8));

    new Timer(50, e -> {
      repaint();
    }).start();
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    for (int i = 0; i < zxScreenComponent.size(); i++) {
      GameTile gameTile = zxScreenComponent.get(i);

      if (gameTile.x == mainGameTile.x && gameTile.y == mainGameTile.y) {
        gameTile = mainGameTile;
      }
      BufferedImage screenBuffer1 = gameTile.zxScreenComponent.screenBuffer;
      int height = getHeight();
      screenBuffer1 = screenBuffer1.getSubimage(0, 0, 256, 128);
      g.drawImage(screenBuffer1, gameTile.x * 256, gameTile.y * 128, screenBuffer1.getWidth(), screenBuffer1.getHeight(), null);
    }
  }

}

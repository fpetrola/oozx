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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.memory.MemoryWriteListener;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ZXScreenComponent extends JComponent implements MemoryWriteListener {
  private final BufferedImage screenBuffer = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
  private final ZxAttribute[][] attributes = new ZxAttribute[24][32];
  private boolean flashState;

  public ZXScreenComponent(int[] memory) {
    for (int y = 0; y < 24; y++)
      for (int x = 0; x < 32; x++) {
        int cellX = x * 8, cellY = y * 8;
        attributes[y][x] = new ZxAttribute((zxColor, value, line, bit) -> screenBuffer.setRGB(cellX + bit, cellY + line, zxColor.getStateColor(((value >> 7 - bit & 1) != 0) != (zxColor.FLASH && flashState)).getRGB()));
      }
    for (int address = 0x4000; address < 0x5B00; address++)
      writtingMemoryAt(address, memory[address]);

    setPreferredSize(new Dimension(256 * 2, 192 * 2));
    new Timer(20, e -> repaint()).start();
    new Timer(320, e -> {
      flashState = !flashState;
      for (ZxAttribute[] row : attributes)
        for (ZxAttribute attribute : row)
          if (attribute.getZxColor().FLASH)
            attribute.redraw();
    }).start();
  }

  public void writtingMemoryAt(int address, int value) {
    if (address >= 0x4000 && address < 0x5800) {
      int y = (address & 0x0700) >> 8 | (address & 0xE0) >> 2 | (address & 0x1800) >> 5;
      attributes[y / 8][address & 0x1F].updateLine(y % 8, value);
    } else if (address >= 0x5800 && address < 0x5B00) {
      int offset = address - 0x5800;
      attributes[offset / 32][offset % 32].setZxColor(new ZxColor(value));
    }
  }

  protected void paintComponent(Graphics g) {
    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
  }
}

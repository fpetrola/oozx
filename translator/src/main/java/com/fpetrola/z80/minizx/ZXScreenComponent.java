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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

public class ZXScreenComponent extends JComponent {
  private static final int SCREEN_WIDTH = 256;
  private static final int SCREEN_HEIGHT = 192;
  private static final int ATTR_WIDTH = 32;
  private static final int ATTR_HEIGHT = 24;

  private final BufferedImage screenBuffer;
  private final int[][] attributes; // Attribute storage: [row][col]
  private final int zoom = 1;

  public ZXScreenComponent() {
    screenBuffer = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
    attributes = new int[ATTR_HEIGHT][ATTR_WIDTH];
    setPreferredSize(new Dimension(256 * zoom, 192 * zoom));
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent arg0) {
        int W = 4;
        int H = 3;
        Rectangle b = arg0.getComponent().getBounds();
        arg0.getComponent().setBounds(b.x, b.y, b.width, b.width * H / W);
      }
    });
  }

  public void onMemoryWrite(int address, int value) {
    if (address >= 0x4000 && address <= 0x57FF) {
      int x = address & 0x001F;
      int y = ((address & 0x0700) >> 8) | ((address & 0xE0) >> 2) | ((address & 0x1800) >> 5);

      for (int bit = 0; bit < 8; bit++) {
        boolean isSet = (value & (1 << (7 - bit))) != 0;
        int color = getPixelColor(y / 8, x, isSet);
        screenBuffer.setRGB((x * 8) + bit, y, color);
      }
    } else if (address >= 0x5800 && address <= 0x5AFF) {
      int attributeOffset = address - 0x5800;
      int attributeRow = attributeOffset / 32;
      int attributeCol = attributeOffset % 32;
      attributes[attributeRow][attributeCol] = value;
    }

    repaint();
  }


  private int getPixelColor(int attrRow, int attrCol, boolean isSet) {
    int attr = attributes[attrRow][attrCol];
    int ink = attr & 0b00000111; // Foreground color
    int paper = (attr >> 3) & 0b00000111; // Background color
    boolean bright = (attr & 0b01000000) != 0;

    Color[] colors = bright ? BRIGHT_COLORS : STANDARD_COLORS;
    return colors[isSet ? ink : paper].getRGB();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
  }

  // ZX Spectrum colors
  private static final Color[] STANDARD_COLORS = {
      new Color(0, 0, 0),       // Black
      new Color(0, 0, 192),     // Blue
      new Color(192, 0, 0),     // Red
      new Color(192, 0, 192),   // Magenta
      new Color(0, 192, 0),     // Green
      new Color(0, 192, 192),   // Cyan
      new Color(192, 192, 0),   // Yellow
      new Color(192, 192, 192)  // White
  };

  private static final Color[] BRIGHT_COLORS = {
      new Color(0, 0, 0),       // Black
      new Color(0, 0, 255),     // Bright Blue
      new Color(255, 0, 0),     // Bright Red
      new Color(255, 0, 255),   // Bright Magenta
      new Color(0, 255, 0),     // Bright Green
      new Color(0, 255, 255),   // Bright Cyan
      new Color(255, 255, 0),   // Bright Yellow
      new Color(255, 255, 255)  // Bright White
  };

  public MemoryWriteListener<WordNumber> getWriteListener() {
    return (address, value) -> onMemoryWrite(address.intValue(), value.intValue());
  }
}

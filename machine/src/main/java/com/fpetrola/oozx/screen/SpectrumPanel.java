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

import com.sun.jna.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public class SpectrumPanel extends JPanel {

  private BufferedImage image;

  public SpectrumPanel(int w, int h) {
    this.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    setPreferredSize(new Dimension(w, h));
  }

  public void updateFrame(Pointer data, int width, int height, long pitch) {
    byte[] row = new byte[(int) pitch];
    int[] pixels = new int[width * height];

    for (int y = 0; y < height; y++) {
      if (data != null) {
        data.read(y * pitch, row, 0, row.length);

        for (int x = 0; x < width; x++) {
          int i = x * 2; // 2 bytes por pixel
          if (i + 1 < row.length) {
            int lo = row[i] & 0xFF;
            int hi = row[i + 1] & 0xFF;
            int rgb565 = (hi << 8) | lo;

            int r = ((rgb565 >> 11) & 0x1F) << 3;
            int g = ((rgb565 >> 5) & 0x3F) << 2;
            int b = (rgb565 & 0x1F) << 3;

            pixels[y * width + x] =
                (0xFF << 24) | (r << 16) | (g << 8) | b;
          }
        }
      }
    }

    image.setRGB(0, 0, width, height, pixels, 0, width);
//    System.out.println("hola1");
    SwingUtilities.invokeLater(this::repaint);
  }


  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (image != null) {
      g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }
  }
}

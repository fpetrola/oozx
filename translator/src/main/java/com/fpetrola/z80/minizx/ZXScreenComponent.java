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
import java.awt.image.BufferedImage;

public class ZXScreenComponent<T extends WordNumber> extends JComponent {

  private final BufferedImage screenBuffer;
  private final ZxAttribute[][] attributes;
  private final SimpleQueue<Runnable> threadSafeQueue;
  private int refresh;

  public ZXScreenComponent() {
    threadSafeQueue = new SimpleQueue<>(10000);

    Thread consumerThread = new Thread(() -> {
      while (true) {
        if (!threadSafeQueue.empty()) {
          Runnable item = threadSafeQueue.poll();
          if (item != null)
            item.run();
        }
      }
    });
    consumerThread.start();

    screenBuffer = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    attributes = new ZxAttribute[24][32];
    for (int y = 0; y < 24; y++) {
      for (int x = 0; x < 32; x++) {
        int finalX = x;
        int finalY = y;
        attributes[y][x] = new ZxAttribute((zxColor, value, line, bit) -> screenBuffer.setRGB(finalX * 8 + bit, finalY * 8 + line, zxColor.getStateColor((value >> 7 - bit & 1) != 0).getRGB()));
      }
    }

    setPreferredSize(new Dimension(256 * 2, 192 * 2));
    addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent event) {
        Rectangle b = event.getComponent().getBounds();
        event.getComponent().setBounds(b.x, b.y, b.width, b.width * 3 / 4);
      }
    });
  }

  public void onMemoryWrite(int address, int value) {
    if (address >= 0x4000 && address <= 0x57FF) {
      int y = (address & 0x0700) >> 8 | (address & 0xE0) >> 2 | (address & 0x1800) >> 5;
      attributes[y / 8][address & 0x001F].updateLine(y % 8, value);
    } else if (address >= 0x5800 && address <= 0x5AFF) {
      int attributeOffset = address - 0x5800;
      attributes[attributeOffset / 32][attributeOffset % 32].setZxColor(new ZxColor(value));
    }

    if (refresh++ % 200 == 0)
      repaint();
  }


  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
  }

  public MemoryWriteListener<T> getWriteListener() {
    return (address, value) -> {
      int address1 = address.intValue();
      int value1 = value.intValue();

      threadSafeQueue.add(() -> onMemoryWrite(address1, value1));
    };
  }
}

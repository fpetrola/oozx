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

import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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

    if (refresh++ % 2 == 0)
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

  public static class ZxAttribute {
    private final LineUpdater lineUpdater;
    private Map<Integer, Integer> pixelValues = new HashMap<>();
    private ZxColor zxColor;
  
    public ZxAttribute(LineUpdater lineUpdater) {
      this.lineUpdater = lineUpdater;
      this.zxColor = new ZxColor(0);
    }
  
    public void setZxColor(ZxColor zxColor) {
      if (this.zxColor.getAttribute() != zxColor.getAttribute()) {
        this.zxColor = zxColor;
        for (int line = 0; line < 8; line++)
          updatePixels(line,  pixelValues.get(line));
      }
    }
  
    private void updatePixels(int line, Integer pixelsValue) {
      for (int bit = 0; bit < 8 && pixelsValue != null; bit++) {
        lineUpdater.update(zxColor, pixelsValue, line, bit);
      }
    }
  
    public void updateLine(int line, int pixelsValue) {
      pixelValues.put(line, pixelsValue);
      updatePixels(line, pixelsValue);
    }
  
    public interface LineUpdater {
      void update(ZxColor zxColor, int value, int line, int bit);
    }
  }

  public static class ZxColor {
  
    public static Color[] colors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};
  
    boolean FLASH;
    boolean BRIGHT;
    byte PAPER;
    byte INK;
  
    public int getAttribute() {
      return attribute;
    }
  
    private final int attribute;
  
    public ZxColor(int attribute) {
      this.FLASH = (attribute & 0x80) != 0;
      this.BRIGHT = (attribute & 0x40) != 0;
      this.PAPER = (byte) ((attribute >> 3) & 0x07);
      this.INK = (byte) (attribute & 0x07);
      this.attribute = attribute;
    }
  
    public Color getInkColor() {
      return BRIGHT ? colors[INK] : colors[INK].darker();
    }
  
    public Color getPaperColor() {
      return BRIGHT ? colors[PAPER] : colors[PAPER].darker();
    }
  
    public Color getStateColor(boolean set) {
      Color color = set ? getInkColor() : getPaperColor();
  //    color= Color.WHITE;
      return color;
    }
  }


}

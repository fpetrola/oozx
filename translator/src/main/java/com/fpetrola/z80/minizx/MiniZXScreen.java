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
import java.awt.geom.AffineTransform;
import java.util.function.Function;

public class MiniZXScreen<T extends WordNumber> extends JPanel {

  protected final Function<Integer, Integer> screenMemory;
  protected final byte[] newScreen;
  protected boolean flashState = false;
  private double zoom = 2;

  public MiniZXScreen(Function<Integer, Integer> screenMemory) {
    this.screenMemory = screenMemory;
    this.newScreen = new byte[256 * 192];
    setPreferredSize(new Dimension((int) (256 * zoom), (int) (192 * zoom)));

    new Timer(30, e -> {
      convertScreen();
      repaint();
    }).start();

    new Timer(300, e -> {
      flashState = !flashState;
    }).start();

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        zoom = (e.getComponent().getSize().getWidth() / 256f);
      }
    });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics2D g2d = (Graphics2D) g.create();

    double width = getWidth();
    double height = getHeight();

    double zoomWidth = width * zoom;
    double zoomHeight = height * zoom;

    double anchorx = (width - zoomWidth) / 2f;
    double anchory = (height - zoomHeight) / 2f;

    AffineTransform at = new AffineTransform();
    //at.translate(anchorx, anchory);
    at.scale(zoom, zoom);
    //  at.translate(-10 * zoom, -10 * zoom);

    g2d.setTransform(at);

    for (int i = 0; i < newScreen.length; i++) {
      double x = i % 256;
      double y = i / 256;

      int zxColorCode = newScreen[i];
      g2d.setColor(zxColorCode >= 8 ? ZxColor.colors[zxColorCode - 8] : ZxColor.colors[zxColorCode].darker());
      g2d.fillRect((int) x, (int) y, 1, 1);
    }
    g2d.dispose();
  }

  protected void convertScreen() {
//    Arrays.fill(newScreen, (byte) 0);

    for (int block = 0; block < 3; block++) {
      int blockAddrOffset = block * 2048;
      int address = 0;
      int line = 0;
      int offset = 0;

      for (int byteRow = 0; byteRow < 2048; byteRow += 32) {
        for (int b = 0; b < 32; b++) {
          byte bite = (byte) screenMemory.apply(16384 + blockAddrOffset + byteRow + b).intValue();

          byte[] pixels = byteToBits(bite);
          for (int pixel = 7; pixel >= 0; pixel--) {
            writeColourPixelToNewScreen(pixels[pixel], blockAddrOffset * 8 + address);
            address++;
          }
        }

        address += 256 * 7;
        line += 1;

        if (line == 8) {
          line = 0;
          offset += 1;
          address = offset * 256;
        }
      }
    }
  }

  protected void writeColourPixelToNewScreen(byte pixel, int newScreenAddress) {
    ZxColor zxColor = new ZxColor((byte) screenMemory.apply(22528 + (newScreenAddress / 2048) * 32 + (newScreenAddress / 8) % 32).intValue());
//    Colour colour = Colour.colourFromAttribute((byte) 2);
//    zxColor = new ZxColor(7);

    byte paperColour = zxColor.PAPER;
    byte inkColour = zxColor.INK;

    if (zxColor.FLASH && flashState) {
      byte newINK = paperColour;
      paperColour = inkColour;
      inkColour = newINK;
    }

    byte colourID = paperColour;
    if (pixel == 1) {
      colourID = inkColour;
    }

    if (zxColor.BRIGHT) {
      colourID += 8;
    }

    if (newScreen[newScreenAddress] != colourID) {
      newScreen[newScreenAddress] = colourID;
    }
  }

  protected byte[] byteToBits(byte b) {
    byte[] bits = new byte[8];
    for (int i = 0; i < 8; i++) {
      bits[i] = (byte) ((b >> i) & 1);
    }
    return bits;
  }

  public MemoryWriteListener getMemoryListener() {
    return (MemoryWriteListener<T>) (address, value) -> {
      int address1 = address.intValue();
      if (address1 >= 16384 && address1 <= 16384 + 6912) {

        int[] ints = memoryToCartesian(address1);

        byte[] pixels = byteToBits((byte) value.intValue());
        for (int pixel = 7; pixel >= 0; pixel--) {
          int blockAddrOffset = ints[0];
          writeColourPixelToNewScreen(pixels[pixel], blockAddrOffset + pixel);
        }
//        convertScreen();

//        System.out.println("sdgdag");
      }
    };

  }

  public int[] memoryToCartesian(int address) {
    // Check if the address is within the valid range
    if (address < 0x4000 || address > 0x57FF) {
      throw new IllegalArgumentException("Address out of screen memory range (0x4000 to 0x57FF)");
    }

    // Calculate the base address relative to 0x4000
    int baseAddress = address - 0x4000;

    // Calculate the logical row based on ZX Spectrum screen layout
    int rowWithinSection = (baseAddress % 0x800) / 32; // Row within 8-row block
    int section = baseAddress / 0x800; // Determine the section (0, 1, 2)
    int logicalRow = (rowWithinSection & 0b111)         // Bottom 3 bits (row within the block)
        + ((rowWithinSection >> 3) * 8)      // Combine middle bits for block offset
        + section * 64;                     // Add section offset

    // Calculate the column (x-coordinate)
    int column = (baseAddress % 32) * 8; // Each byte contains 8 pixels

    return new int[]{column, logicalRow};
  }

}

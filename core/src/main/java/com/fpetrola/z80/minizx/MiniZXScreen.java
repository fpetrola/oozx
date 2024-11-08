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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.function.Function;

public class MiniZXScreen extends JPanel {

  protected final Function<Integer, Integer> screenMemory;
  protected final byte[] newScreen;
  protected boolean flashState = false;
  private double zoom = 2;
  Color[] colors = {Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.YELLOW, Color.WHITE};

  public MiniZXScreen(Function<Integer, Integer> screenMemory) {
    this.screenMemory = screenMemory;
    this.newScreen = new byte[256 * 192];
    setPreferredSize(new Dimension((int) (256 * zoom), (int) (192 * zoom)));

    new Timer(10, e -> {
      convertScreen();
      repaint();
    }).start();

    new Timer(500, e -> {
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
      g2d.setColor(zxColorCode >= 8 ? colors[zxColorCode - 8] : colors[zxColorCode].darker());
      g2d.fillRect((int) x, (int) y, 1, 1);
    }
    g2d.dispose();
  }

  protected void convertScreen() {
    Arrays.fill(newScreen, (byte) 0);

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
    Colour colour = Colour.colourFromAttribute((byte) screenMemory.apply(22528 + (newScreenAddress / 2048) * 32 + (newScreenAddress / 8) % 32).intValue());

    byte paperColour = colour.PAPER;
    byte inkColour = colour.INK;

    if (colour.FLASH && flashState) {
      byte newINK = paperColour;
      paperColour = inkColour;
      inkColour = newINK;
    }

    byte colourID = paperColour;
    if (pixel == 1) {
      colourID = inkColour;
    }

    if (colour.BRIGHT) {
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

  protected static class Colour {
    boolean FLASH;
    boolean BRIGHT;
    byte PAPER;
    byte INK;

    protected static Colour colourFromAttribute(byte attribute) {
      Colour colour = new Colour();

      colour.FLASH = (attribute & 0x80) != 0;
      colour.BRIGHT = (attribute & 0x40) != 0;
      colour.PAPER = (byte) ((attribute >> 3) & 0x07);
      colour.INK = (byte) (attribute & 0x07);

      return colour;
    }
  }
}

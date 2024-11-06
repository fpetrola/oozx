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

import com.fpetrola.z80.mmu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("ALL")
public abstract class MiniZX extends SpectrumApplication {
  public MiniZX() {
    init();
  }

  public void init() {
    this.mem = new int[65536];
    this.createScreen();
    final byte[] rom = gzipDecompressFromBase64("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");
    final byte[] bytes = gzipDecompressFromBase64(this.getProgramBytes());
    for (int i = 0; i < 65536; ++i) {
      this.getMem()[i] = ((i < 16384) ? rom[i] : bytes[i]) & 0xff;
    }

    customizeMemory();

    syncChecker.init(this);
  }

  protected void customizeMemory() {
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> syncChecker.getByteFromEmu(index);
  }

  protected abstract String getProgramBytes();

  public byte[] gzipDecompressFromBase64(final String content) {
    if (StringUtils.isBlank(content)) {
      throw new IllegalArgumentException("content is either null or blank");
    }

    byte[] decode = Base64.getDecoder().decode(content.getBytes(UTF_8));
    try (ByteArrayInputStream bis = new ByteArrayInputStream(decode)) {
      try (GZIPInputStream gis = new GZIPInputStream(bis)) {
        byte[] bytes = gis.readAllBytes();
        return bytes;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createScreen() {
    JFrame frame = new JFrame("Mini ZX Spectrum");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(new MiniZXScreen(getMemFunction()));
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(io.keyboard);
//    frame.addKeyListener(new KeyListener() {
//      public void keyTyped(KeyEvent e) {
//      }
//
//      public void keyPressed(KeyEvent e) {
//        io.setCurrentKey(e.getKeyCode(), true);
//      }
//
//      public void keyReleased(KeyEvent e) {
//        io.setCurrentKey(e.getKeyCode(), false);
//      }
//    });
  }

  public static class MiniZXIO implements IO<WordNumber> {
    private int[] ports = initPorts();
    private LinkedList<PortInput> lastEmuInputs = new LinkedList<>();
    private LinkedList<PortInput> lastJavaInputs = new LinkedList<>();
    public Keyboard keyboard;

    public MiniZXIO() {
      keyboard = new Keyboard();
    }

    public synchronized WordNumber in(WordNumber port) {
      WordNumber wordNumber = processLastInputs(port, lastEmuInputs, lastJavaInputs);
//      System.out.println("emu IN: " + port.intValue() + "= " + wordNumber.intValue());
      return wordNumber;
    }

    private WordNumber in0(WordNumber port) {
      WordNumber value = WordNumber.createValue(performIn(port.intValue()));
      return value;
//      int portNumber = port.intValue();
//      //  portNumber = portNumber & 0xff;
//      int port1 = ports[portNumber];
//      WordNumber value = WordNumber.createValue(port1);
//
////      if (portNumber == 31 && value.intValue() != 0)
////        ports[31] = 0;
//
//      return value;

//      if (port1 != 0) {
//        //      System.out.println(port1);
//        return WordNumber.createValue(port1);
//      } else {
//        if (portNumber == 31)
//          return WordNumber.createValue(0);
//        else
//          return WordNumber.createValue(191);
//      }
    }

    public void out(WordNumber port, WordNumber value) {
    }

    public void setCurrentKey(int e, boolean pressed) {
      if (KeyEvent.VK_RIGHT == e) {
        activateKey(1, pressed);
//        ports[getAnInt(61438)] = pressed ? 187 : 255;
//        ports[getAnInt(59390)] = pressed ? 187 : 255;
      } else if (KeyEvent.VK_LEFT == e) {
        activateKey(2, pressed);
//        ports[getAnInt(61438)] = pressed ? 175 : 255;
//        ports[getAnInt(59390)] = pressed ? 175 : 255;
      } else if (KeyEvent.VK_UP == e) {
        activateKey(8, pressed);
      } else if (KeyEvent.VK_DOWN == e) {
        activateKey(4, pressed);
      } else if (KeyEvent.VK_SPACE == e) {
        activateKey(16, pressed);
//        ports[getAnInt(61438)] = pressed ? 254 : 255;
      } else if (KeyEvent.VK_ENTER == e) {
        activateKey(16, pressed);

//        ports[getAnInt(49150)] = pressed ? 254 : 255;
//        ports[getAnInt(45054)] = pressed ? 190 : 255;
//        ports[getAnInt(61438)] = pressed ? 254 : 255;
      }
    }

    private int getAnInt(int i) {
      //System.out.println(i & 0xff);
      return i;
    }

    private void activateKey(int i, boolean pressed) {
      if (pressed)
        ports[31] |= i;
      else {
        int i1 = ~i;
        ports[31] &= i1;
      }
    }

    private int[] initPorts() {
      int[] ports = new int[0x10000];
      Arrays.fill(ports, 0);
      //    ports[65278]= 191;
      //    ports[32766]= 191;
      //    ports[65022]= 191;
      //    ports[49150]= 191;
      //    ports[61438]= 191;
      //    ports[64510]= 191;
      //    ports[59390]= 191;
      //    ports[59390]= 191;
      //    ports[59390]= 191;
      ports[45054] = 1;
      return ports;
    }

    public synchronized WordNumber in2(WordNumber port) {
      WordNumber wordNumber = processLastInputs(port, lastJavaInputs, lastEmuInputs);
//      System.out.println("java IN: " + port.intValue() + "= " + wordNumber.intValue());
      return wordNumber;
    }

    private WordNumber processLastInputs(WordNumber port, LinkedList<PortInput> ownInputs, LinkedList<PortInput> otherInputs) {
      if (otherInputs.isEmpty()) {
        WordNumber in = in0(port);
        ownInputs.offer(new PortInput(port, in));
        return in;
      } else {
        PortInput pop = otherInputs.poll();
//        if (pop.port.intValue() != port.intValue())
//          System.out.println("port!");

        return pop.result;
      }
    }

    private int performIn(int port) {
      if ((port & 0x0001) == 0) {
        int earBit = 191;
        int i = keyboard.readKeyboardPort(port, true) & earBit;
//        if (i != 191)
        //   System.out.println("port: " + port + " -> " + i);
        return i;
      }

      return 0 & 0xff;
    }
  }

  public static class MiniZXScreen extends JPanel {

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
}

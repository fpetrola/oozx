/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.tools.games;

import com.fpetrola.oozx.speccy.modules.display.Display;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * A game's screenshots, drawn at whatever size the space allows: two side by side in a row of
 * search results, one on its own in a tile of the gallery.
 * <p>
 * They used to be icons in labels, and an icon fixes a label's size at the pixels it happens to
 * have: widening the window moved the empty space around rather than the picture. This asks its
 * parent how wide the row is and paints into that, so the pictures follow the window.
 * <p>
 * Nearest neighbour on purpose. These are 256x192 loading screens, and interpolating them turns
 * the pixel art to mush; scaled hard they stay sharp, which is how a Spectrum screen is meant to
 * look enlarged.
 */
public class Screenshots extends JComponent {

  /**
   * What a Spectrum screen is, and so the shape to reserve before an image has arrived. Taken from
   * the display the emulator draws rather than written out again: the paper is 32 columns by 24
   * rows of eight pixels, and the border around it is four columns at the sides and three at the
   * top and bottom, which is why it is not the same thickness all the way round.
   */
  private static final int SCREEN_WIDTH = (Display.SCREEN_WIDTH_COLS - 2 * Display.BORDER_WIDTH_COLS) * 8;
  private static final int SCREEN_HEIGHT = Display.HEIGHT;
  private static final int FRAME_WIDTH = Display.SCREEN_WIDTH_COLS * 8;
  private static final int FRAME_HEIGHT = Display.SCREEN_HEIGHT;
  private static final int GAP = 10;
  /** The stripes a tape's pilot tone puts in the border, which are red and cyan. */
  private static final Color LOADING_RED = new Color(0xD8, 0x00, 0x00);
  private static final Color LOADING_CYAN = new Color(0x00, 0xD8, 0xD8);
  /** As thick as a character is tall, which is what the stripes of a real pilot tone look like. */
  private static final int STRIPE = 8;
  private static final byte[] CHARACTERS = charactersOfTheRom();

  private final BufferedImage[] shots;
  private final boolean[] coming;
  private boolean anyFailed;

  public Screenshots(MouseAdapter mouseAdapter, String... urls) {
    setOpaque(false);
    shots = new BufferedImage[Math.max(1, urls.length)];
    coming = new boolean[shots.length];
    if (mouseAdapter != null) addMouseListener(mouseAdapter);
    for (int slot = 0; slot < urls.length; slot++) {
      show(slot, urls[slot]);
    }
  }

  /** Puts a picture in one of the slots, which for a game found on disk arrives after the tile. */
  public void show(int slot, String url) {
    if (url == null) {
      return;
    }
    coming[slot] = true;
    new SwingWorker<BufferedImage, Void>() {
      protected BufferedImage doInBackground() throws Exception {
        return ImageIO.read(new URL(url));
      }

      protected void done() {
        try {
          shots[slot] = get();
        } catch (Exception e) {
          anyFailed = true;
        }
        // The height depends on the picture's shape, so the row has to be measured again.
        revalidate();
        repaint();
      }
    }.execute();
  }

  /**
   * As wide as the row allows, and as tall as that width makes the pictures. Asking the parent
   * is what ties the two together: Swing hands out preferred sizes before it assigns bounds, so
   * a component that wants to grow with its container has to look at the container.
   */
  @Override
  public Dimension getPreferredSize() {
    int available = getParent() == null
        ? SCREEN_WIDTH * shots.length + GAP * (shots.length - 1) : getParent().getWidth();
    return new Dimension(available, heightOf(widthOfEach(available)));
  }

  private int widthOfEach(int available) {
    return Math.max(1, (available - GAP * (shots.length - 1)) / shots.length);
  }

  private int heightOf(int width) {
    for (BufferedImage sample : shots) {
      if (sample != null) {
        return width * sample.getHeight() / sample.getWidth();
      }
    }
    return width * SCREEN_HEIGHT / SCREEN_WIDTH;
  }

  @Override
  protected void paintComponent(Graphics g) {
    int each = widthOfEach(getWidth());

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    for (int slot = 0; slot < shots.length; slot++) {
      int x = slot * (each + GAP);
      BufferedImage shot = shots[slot];
      if (shot != null) {
        g2.drawImage(shot, x, 0, each, heightOf(each), null);
      } else {
        paintPlaceholder(g2, x, each);
      }
    }
    g2.dispose();
  }

  /**
   * What the machine itself shows while a tape is going in: the border striping, a black screen,
   * and the ROM's own line about what is loading, in the ROM's own letters. A game with no picture
   * of its own is drawn as the one screen every Spectrum game has been seen on.
   * <p>
   * The letters are drawn at the size they are on the machine and never scaled: eight pixels is
   * already small and a fraction of that is a smear. What gives way when the tile is too narrow for
   * the line is the line itself, which loses the word the ROM prints before a name; the border
   * keeps its proportions, which is what makes the thing look like a Spectrum and not like a frame.
   */
  private void paintPlaceholder(Graphics2D g2, int x, int width) {
    int height = heightOf(width);
    String message = "Program: " + (coming[slotAt(x, width)] && !anyFailed ? "loading..." : "no screenshot");
    if (message.length() * 8 + 8 > width * SCREEN_WIDTH / FRAME_WIDTH) {
      message = message.substring("Program: ".length());
    }
    // The border of a Spectrum frame, which is wider at the sides than above and below.
    int sides = width * Display.BORDER_WIDTH_COLS * 8 / FRAME_WIDTH;
    int ends = height * Display.BORDER_HEIGHT / FRAME_HEIGHT;

    for (int y = 0; y < height; y += STRIPE) {
      g2.setColor((y / STRIPE) % 2 == 0 ? LOADING_RED : LOADING_CYAN);
      g2.fillRect(x, y, width, Math.min(STRIPE, height - y));
    }
    g2.setColor(Color.BLACK);
    g2.fillRect(x + sides, ends, width - sides * 2, height - ends * 2);
    if (message.length() * 8 <= width - sides * 2) {
      inTheRomsLetters(g2, message, x + sides + 4, ends + 4);
    }
  }

  private int slotAt(int x, int width) {
    return Math.min(coming.length - 1, x / Math.max(1, width + GAP));
  }

  /**
   * A string in the Spectrum's own characters, drawn from the 48K ROM's set at 0x3D00. Eight bytes
   * each, one per row, from the space up; anything the machine has no letter for is skipped.
   */
  private static void inTheRomsLetters(Graphics2D g2, String text, int x, int y) {
    if (CHARACTERS == null) {
      g2.setColor(Color.WHITE);
      g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8));
      g2.drawString(text, x, y + 7);
      return;
    }
    g2.setColor(Color.WHITE);
    for (int letter = 0; letter < text.length(); letter++) {
      int character = text.charAt(letter) - ' ';
      if (character < 0 || character >= 96) {
        continue;
      }
      for (int row = 0; row < 8; row++) {
        int bits = CHARACTERS[character * 8 + row] & 0xFF;
        for (int column = 0; column < 8; column++) {
          if ((bits & (0x80 >> column)) != 0) {
            g2.fillRect(x + letter * 8 + column, y + row, 1, 1);
          }
        }
      }
    }
  }

  /** The character set of the 48K ROM, or null where that ROM is not on the classpath. */
  private static byte[] charactersOfTheRom() {
    try (java.io.InputStream rom = Screenshots.class.getResourceAsStream("/roms/48.rom")) {
      if (rom == null) {
        return null;
      }
      byte[] all = rom.readAllBytes();
      return all.length < 0x3D00 + 96 * 8 ? null : java.util.Arrays.copyOfRange(all, 0x3D00, 0x3D00 + 96 * 8);
    } catch (java.io.IOException withoutTheRom) {
      return null;
    }
  }
}

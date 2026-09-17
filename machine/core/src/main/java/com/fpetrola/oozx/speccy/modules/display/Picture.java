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

package com.fpetrola.oozx.speccy.modules.display;

import com.google.inject.Singleton;

/**
 * The RGB pixel buffer (border included) that the display paints into directly, so a viewer can wrap it
 * as an image with no copy.
 */
@Singleton
public class Picture {
  /** Eight pixels to a column, as every Sinclair draws it. */
  public static final int WIDTH = Display.SCREEN_WIDTH_COLS * 8;
  /** Sixteen, which is as wide as a column ever gets, and the distance between one row and the next. */
  public static final int STRIDE = Display.SCREEN_WIDTH_COLS * 16;
  public static final int HEIGHT = Display.SCREEN_HEIGHT;

  /** Indices 0-7 are normal (channel value 0xB2), 8-15 the bright versions (0xFF). */
  public static final int[] SINCLAIR = new int[16];

  /** How many colours a picture can be painted in, which is as many as anything here can ask for. */
  public static final int COLOURS = 256;

  static {
    for (int colour = 0; colour < 8; colour++) {
      int bright = ((colour & 1) != 0 ? 0x0000ff : 0) | ((colour & 2) != 0 ? 0xff0000 : 0) | ((colour & 4) != 0 ? 0x00ff00 : 0);
      SINCLAIR[colour] = bright & 0xB2B2B2;
      SINCLAIR[8 + colour] = bright;
    }
  }

  /**
   * What each colour index is worth. The sixteen a Sinclair has, over and over, until something
   * gives a machine more of them and says what they are: a colour is an index here and nothing
   * else, so who filled it in is not asked at the time of drawing a pixel.
   */
  public final int[] palette = new int[COLOURS];

  {
    sinclairColours();
  }

  /** The sixteen back, in every one of the four tables a byte can name. */
  public void sinclairColours() {
    for (int colour = 0; colour < COLOURS; colour++) palette[colour] = SINCLAIR[colour & 0x0f];
  }

  public void colour(int index, int rgb) {
    palette[index & (COLOURS - 1)] = rgb;
  }

  public final int[] pixels = new int[STRIDE * HEIGHT];

  /**
   * Whether anything is being drawn at all. Honoured where the drawing starts - the screen once a
   * line and the border once a frame - and not here, so that plotting a cell costs nothing to ask.
   */
  public boolean active = true;

  /**
   * How many pixels a column is worth. Eight everywhere, until a machine says it can put sixteen
   * in the width of one, and then everything drawn by the column - the border included - is twice
   * as wide without knowing it.
   */
  private int columnWidth = 8;

  public int columnWidth() {
    return columnWidth;
  }

  public int width() {
    return Display.SCREEN_WIDTH_COLS * columnWidth;
  }

  public void columnWidth(int pixels) {
    columnWidth = pixels;
  }

  /**
   * Eight pixels of a bitmap byte in two colours. Eight and not {@link #columnWidth}, because a
   * machine that draws its columns wider draws them with {@link #plot16} and borders them with
   * {@link #fillColumn}, and this is what is left: the way every Sinclair draws.
   */
  public void plot8(int x, int y, byte data, byte ink, byte paper) {
    int at = y * STRIDE + x * 8;
    int inkColour = palette[ink & 0xff], paperColour = palette[paper & 0xff];
    for (int i = 0; i < 8; i++) {
      pixels[at + i] = (data & (0x80 >> i)) != 0 ? inkColour : paperColour;
    }
  }

  /** A whole column of the one colour, however wide a column is, which is what a border is made of. */
  public void fillColumn(int x, int y, byte colour) {
    int at = y * STRIDE + x * columnWidth;
    java.util.Arrays.fill(pixels, at, at + columnWidth, palette[colour & 0xff]);
  }

  /** Two pixels of their own colours, one pair of the four a column is made of when a byte is a colour. */
  public void plotPair(int x, int y, int pair, byte left, byte right) {
    paintPair(x, y, pair, palette[left & 0xff], palette[right & 0xff]);
  }

  /** The same two pixels in colours that are in no palette, for whoever worked them out itself. */
  public void paintPair(int x, int y, int pair, int left, int right) {
    int at = y * STRIDE + x * 8 + pair * 2;
    pixels[at] = left;
    pixels[at + 1] = right;
  }

  /** Sixteen pixels of their own, from the two bytes a column is made of where one is not enough. */
  public void plot16(int x, int y, int data, byte ink, byte paper) {
    int at = y * STRIDE + (x << 4);
    int inkColour = palette[ink & 0xff], paperColour = palette[paper & 0xff];
    for (int i = 0; i < 16; i++) {
      pixels[at + i] = (data & (0x8000 >> i)) != 0 ? inkColour : paperColour;
    }
  }
}

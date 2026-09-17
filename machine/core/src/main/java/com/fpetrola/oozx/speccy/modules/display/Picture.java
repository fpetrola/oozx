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

  /** The pixels themselves, for whoever draws a column in a way only its own machine draws it. */
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
   * Eight pixels of a bitmap byte in two colours, which is the way every Sinclair draws and the
   * only way this knows. Eight and not {@link #columnWidth}: a machine that makes a column out of
   * something else makes it out of {@link #pixels} itself, where its own way belongs.
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
}

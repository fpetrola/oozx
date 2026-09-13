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
  public static final int[] PALETTE = new int[16];

  static {
    for (int colour = 0; colour < 8; colour++) {
      int bright = ((colour & 1) != 0 ? 0x0000ff : 0) | ((colour & 2) != 0 ? 0xff0000 : 0) | ((colour & 4) != 0 ? 0x00ff00 : 0);
      PALETTE[colour] = bright & 0xB2B2B2;
      PALETTE[8 + colour] = bright;
    }
  }

  public final int[] pixels = new int[STRIDE * HEIGHT];
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

  public void plot8(int x, int y, byte data, byte ink, byte paper) {
    if (!active) return;
    int at = y * STRIDE + x * columnWidth;
    int inkColour = PALETTE[ink], paperColour = PALETTE[paper];
    if (columnWidth == 8) {
      for (int i = 0; i < 8; i++) {
        pixels[at + i] = (data & (0x80 >> i)) != 0 ? inkColour : paperColour;
      }
      return;
    }
    for (int i = 0; i < 8; i++) {
      int colour = (data & (0x80 >> i)) != 0 ? inkColour : paperColour;
      pixels[at + 2 * i] = colour;
      pixels[at + 2 * i + 1] = colour;
    }
  }

  /** Sixteen pixels of their own, from the two bytes a column is made of where one is not enough. */
  public void plot16(int x, int y, int data, byte ink, byte paper) {
    if (!active) return;
    int at = y * STRIDE + (x << 4);
    int inkColour = PALETTE[ink], paperColour = PALETTE[paper];
    for (int i = 0; i < 16; i++) {
      pixels[at + i] = (data & (0x8000 >> i)) != 0 ? inkColour : paperColour;
    }
  }
}

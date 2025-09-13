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
 * The picture the machine draws: a row of RGB ints per line, border included, plotted into
 * directly by the display. A window wraps the array in an image, so showing a frame is a scale
 * of it and no copy - which is why the picture is here and not with the window: the display
 * paints it, and nothing about painting it is the window's.
 */
@Singleton
public class Picture {
  public static final int WIDTH = Display.SCREEN_WIDTH_COLS * 8;
  public static final int HEIGHT = Display.SCREEN_HEIGHT;

  /** The eight colours and then their bright ones, in RGB: normal is 0xB2 where bright is 0xFF. */
  public static final int[] PALETTE = new int[16];

  static {
    for (int colour = 0; colour < 8; colour++) {
      int bright = ((colour & 1) != 0 ? 0x0000ff : 0) | ((colour & 2) != 0 ? 0xff0000 : 0) | ((colour & 4) != 0 ? 0x00ff00 : 0);
      PALETTE[colour] = bright & 0xB2B2B2;
      PALETTE[8 + colour] = bright;
    }
  }

  public final int[] pixels = new int[WIDTH * HEIGHT];
  public boolean active = true;

  public void plot8(int x, int y, byte data, byte ink, byte paper) {
    if (!active) return;
    int at = y * WIDTH + (x << 3);
    int inkColour = PALETTE[ink], paperColour = PALETTE[paper];
    for (int i = 0; i < 8; i++) {
      pixels[at + i] = (data & (0x80 >> i)) != 0 ? inkColour : paperColour;
    }
  }
}

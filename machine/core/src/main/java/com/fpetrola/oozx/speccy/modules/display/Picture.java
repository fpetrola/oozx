/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
  public static final int WIDTH = Display.SCREEN_WIDTH_COLS * 8;
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

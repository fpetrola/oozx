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

/**
 * ZX Spectrum screen memory layout, precomputed since it never changes.
 * The bitmap is not stored in row order: an offset encodes (third-of-screen, pixel-row-within-char, char-row),
 * so a character's 8 pixel lines are 256 bytes apart and the next character row starts 32 bytes later.
 * The 6144-byte attribute area that follows is laid out in normal row order, one byte per 8x8 cell.
 * Offsets are into the shown bank, not bus addresses (e.g. a 128K can display bank 7 while bank 5 sits at 0x4000).
 */
public final class ScreenLayout {
  public static final int LINES = 192;
  public static final int ATTRIBUTES = 6144;

  public final int[] lineStart = new int[LINES];
  public final int[] attrStart = new int[LINES];

  public ScreenLayout() {
    for (int y = 0; y < LINES; y++) {
      lineStart[y] = 32 * ((y & 0xC0) | ((y & 7) << 3) | ((y & 0x38) >> 3));
      attrStart[y] = ATTRIBUTES + 32 * (y >> 3);
    }
  }

  /** Inverse of {@link #lineStart}: decodes a bitmap offset's (third, char-row, pixel-row) bit fields. */
  public int lineOf(int offset) {
    return ((offset >> 11) & 3) * 64 + ((offset >> 5) & 7) * 8 + ((offset >> 8) & 7);
  }

  public int columnOf(int offset) {
    return offset & 31;
  }

  public int attributeRowOf(int offset) {
    return (offset - ATTRIBUTES) >> 5;
  }
}

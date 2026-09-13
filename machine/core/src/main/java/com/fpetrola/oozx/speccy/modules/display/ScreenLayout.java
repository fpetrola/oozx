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
  /** How far the second display file sits above the first. */
  public static final int SECOND_FILE = 0x2000;

  /** Which display file is being shown: nought for the usual one, {@link #SECOND_FILE} for the other. */
  public int file;

  /**
   * Whether a colour covers one line of a cell instead of eight. When it does, the colour of a
   * byte is at that byte's own address in the other file, which is what gives a Timex machine
   * eight times the colour resolution down the screen for the same bitmap.
   */
  public boolean colourPerLine;

  public int pixelsAt(int line, int column) {
    return file + lineStart[line] + column;
  }

  public int colourAt(int line, int column) {
    return colourPerLine ? SECOND_FILE + lineStart[line] + column : file + attrStart[line] + column;
  }

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

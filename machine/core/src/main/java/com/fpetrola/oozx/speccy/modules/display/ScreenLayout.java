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
 * Where each line of the picture is in the bank that holds it.
 * <p>
 * The bitmap is not in the order it is read in: a line's offset carries the third first, then
 * which line of a character row, then which character row, so the eight lines of one row are 256
 * bytes apart and the row below starts 32 bytes on. The attributes follow the 6144 bytes it takes,
 * one for each cell of eight by eight, and are laid out the way the bitmap is not.
 * <p>
 * These are offsets and not addresses: the beam reads the bank that is the screen, which on a 128
 * can be bank 7 while bank 5 is what sits at 0x4000. It is worked out once because nothing about
 * it ever changes, and it is its own object because two different things need it - the display to
 * plot with, and the machine to answer what is floating on the bus.
 */
public final class ScreenLayout {
  public static final int LINES = 192;
  public static final int ATTRIBUTES = 6144;

  /** Where a pixel line starts. */
  public final int[] lineStart = new int[LINES];

  /** Where the attributes of the character row a line falls in start. */
  public final int[] attrStart = new int[LINES];

  public ScreenLayout() {
    for (int y = 0; y < LINES; y++) {
      lineStart[y] = 32 * ((y & 0xC0) | ((y & 7) << 3) | ((y & 0x38) >> 3));
      attrStart[y] = ATTRIBUTES + 32 * (y >> 3);
    }
  }

  /** Which pixel line an offset into the bitmap belongs to: the way back from {@link #lineStart}. */
  public int lineOf(int offset) {
    return ((offset >> 11) & 3) * 64 + ((offset >> 5) & 7) * 8 + ((offset >> 8) & 7);
  }

  /** Which of the 32 columns it is, which the layout leaves alone. */
  public int columnOf(int offset) {
    return offset & 31;
  }

  /** Which character row an offset into the attributes belongs to. */
  public int attributeRowOf(int offset) {
    return (offset - ATTRIBUTES) >> 5;
  }
}

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

import java.util.Arrays;

/**
 * Which cells of the picture still have to be put on the canvas. A frame plots what changed and
 * not the whole screen, so a write marks its cell and the plotting takes the marks away as it
 * goes; a row is 32 bits, one per column.
 * <p>
 * It says what has to be plotted and nothing about when: catching the beam up before a cell is
 * marked is the display's business, because only the display knows where the beam is.
 */
public final class DirtyCells {
  private final int[] rows;

  public DirtyCells(int lines) {
    rows = new int[lines];
    all();
  }

  /** Everything is to be plotted again: the memory changed with no write, as a snapshot does. */
  public void all() {
    Arrays.fill(rows, -1);
  }

  public void cell(int column, int line) {
    rows[line] |= 1 << column;
  }

  /** The cells of a row that are still to be plotted, between two columns. */
  public int between(int line, int from, int to) {
    return rows[line] & (int) (((1L << to) - 1) & ~((1L << from) - 1));
  }

  /** Those ones are on the canvas now. */
  public void plotted(int line, int cells) {
    rows[line] &= ~cells;
  }
}

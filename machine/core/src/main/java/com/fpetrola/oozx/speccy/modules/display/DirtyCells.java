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
 * Cells not yet redrawn since their last write, one bit per column (32 columns per row). Only tracks what needs plotting, not when — that timing is {@link Display}'s job.
 */
public final class DirtyCells {
  private final int[] rows;

  public DirtyCells(int lines) {
    rows = new int[lines];
    all();
  }

  /** Marks every cell dirty, for when memory changed without going through {@code write} (e.g. loading a snapshot). */
  public void all() {
    Arrays.fill(rows, -1);
  }

  public void cell(int column, int line) {
    rows[line] |= 1 << column;
  }

  public int between(int line, int from, int to) {
    return rows[line] & (int) (((1L << to) - 1) & ~((1L << from) - 1));
  }

  public void plotted(int line, int cells) {
    rows[line] &= ~cells;
  }
}

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

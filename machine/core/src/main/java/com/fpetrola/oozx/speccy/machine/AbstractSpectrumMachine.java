/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.machine;



import com.fpetrola.oozx.speccy.modules.display.Display;

public abstract class AbstractSpectrumMachine implements SpectrumMachine {
  /** The T-state of the first displayed line: every other line is a line's length further on. */
  private long firstLine;
  protected final Paging paging = new Paging();


  public Paging paging() {
    return paging;
  }

  public long lineStart(int line) {
    return firstLine + (long) line * getTimings().tstatesPerLine();
  }

  public void firstLineAt(long tState) {
    firstLine = tState;
  }
}

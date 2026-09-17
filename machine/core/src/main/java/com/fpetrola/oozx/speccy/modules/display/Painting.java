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


package com.fpetrola.oozx.speccy.modules.display;

import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;

import static com.fpetrola.oozx.speccy.modules.display.Display.BORDER_HEIGHT;
import static com.fpetrola.oozx.speccy.modules.display.Display.BORDER_WIDTH_COLS;
import static com.fpetrola.oozx.speccy.modules.display.Display.WIDTH_COLS;

/**
 * Plots dirty cells up to a given beam position, from memory as it stands when each cell is reached
 * (not deferred to frame end). Only told how far to go — it does not track the beam itself.
 */
public final class Painting {
  private final SpectrumMemory banks;
  private final ScreenLayout layout;
  private final DirtyCells dirty;
  private final Colouring colouring;
  private final Picture canvas;
  private int plottedX;
  private int plottedY;

  /**
   * Whoever paints the dirty columns of a line, given as a bit each.
   * <p>
   * There is one of these at a time and it is asked once a line, so what decides between them -
   * a port written, a chip plugged in - is asked then and not once a cell.
   */
  public interface Line {
    void paint(int y, int bits);

    /**
     * Whether its pixels change for reasons the writes this screen watches cannot see, so that a
     * cell nobody wrote to would otherwise keep what it had for good.
     */
    default boolean allOfItEveryFrame() {
      return false;
    }

    /**
     * Whether a cell of this picture can flash at all. It takes an attribute in memory to carry
     * the bit that says so, and a picture whose colours are not attributes has nothing there:
     * what lies where the attributes would be is somebody's bitmap.
     */
    default boolean cellsCanFlash() {
      return true;
    }
  }

  /** A bitmap byte and the two colours of its cell, which is how every Sinclair draws. */
  public final Line sinclair = this::plotSinclair;

  private Line line = sinclair;

  /** Who paints the lines from now on, or nobody for the machine's own way, answering who did. */
  public Line line(Line another) {
    Line who = line;
    line = another == null ? sinclair : another;
    return who;
  }

  public Painting(SpectrumMemory banks, ScreenLayout layout, DirtyCells dirty, Colouring colouring, Picture canvas) {
    this.banks = banks;
    this.layout = layout;
    this.dirty = dirty;
    this.colouring = colouring;
    this.canvas = canvas;
  }

  public boolean past(int x, int y) {
    return y > plottedY || (y == plottedY && x >= plottedX);
  }

  public void upTo(int x, int y) {
    if (y < plottedY || (y == plottedY && x <= plottedX)) {
      return;
    }
    if (plottedY < y) {
      plotLine(plottedY++, plottedX, WIDTH_COLS);
      for (; plottedY < y; plottedY++) {
        plotLine(plottedY, 0, WIDTH_COLS);
      }
      plottedX = 0;
    }
    plotLine(y, plottedX, x);
    plottedX = x;
  }

  /** Whether it is worth looking for cells that flash, which the one painting is the one to say. */
  public boolean cellsCanFlash() {
    return line.cellsCanFlash();
  }

  public void startAgain() {
    plottedX = plottedY = 0;
    if (line.allOfItEveryFrame()) dirty.all();
  }

  private void plotSinclair(int y, int bits) {
    byte[] screen = banks.shown().bytes;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      byte attribute = screen[layout.colourAt(y, x)];
      canvas.plot8(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, screen[layout.pixelsAt(y, x)],
          colouring.ink(attribute), colouring.paper(attribute));
    }
  }

  private void plotLine(int y, int from, int to) {
    if (!canvas.active) return;
    int bits = dirty.between(y, from, to);
    if (bits == 0) {
      return;
    }
    dirty.plotted(y, bits);
    line.paint(y, bits);
  }
}

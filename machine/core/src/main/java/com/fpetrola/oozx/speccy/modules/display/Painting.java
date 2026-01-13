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

  public void startAgain() {
    plottedX = plottedY = 0;
  }

  private void plotLine(int y, int from, int to) {
    int bits = dirty.between(y, from, to);
    if (bits == 0) {
      return;
    }
    dirty.plotted(y, bits);
    byte[] screen = banks.shown().bytes;
    int pixels = layout.lineStart[y], attrs = layout.attrStart[y];
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      byte attribute = screen[attrs + x];
      canvas.plot8(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, screen[pixels + x],
          colouring.ink(attribute), colouring.paper(attribute));
    }
  }
}

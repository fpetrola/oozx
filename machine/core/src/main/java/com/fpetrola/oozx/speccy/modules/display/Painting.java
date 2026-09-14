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
  private PixelsOfItsOwn ofItsOwn;
  private int plottedX;
  private int plottedY;

  /**
   * Whoever paints a column of the screen where the machine's memory is not where its pixels are.
   * <p>
   * The three rules below read the bytes the machine is showing; something that keeps its pixels
   * somewhere else cannot be one of them, and asking it is the only thing the screen has to know
   * about it.
   */
  public interface PixelsOfItsOwn {
    /** The eight pixels of column {@code x} of line {@code y} of the screen, painted on the canvas. */
    void column(int x, int y);
  }

  /** Who paints the columns from now on, or nobody, which is the machine's own three rules. */
  public void pixelsOfItsOwn(PixelsOfItsOwn another) {
    ofItsOwn = another;
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

  public void startAgain() {
    plottedX = plottedY = 0;
  }

  /** A byte that is not a bitmap but two colours: the same bits an attribute puts its ink and paper in. */
  private void plotColours(int x, int y, int pair, byte colours) {
    canvas.plotPair(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, pair,
        Colouring.inkBits(colours), Colouring.paperBits(colours));
  }

  private void plotLine(int y, int from, int to) {
    int bits = dirty.between(y, from, to);
    if (bits == 0) {
      return;
    }
    dirty.plotted(y, bits);
    byte[] screen = banks.shown().bytes;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      if (ofItsOwn != null) {
        ofItsOwn.column(x, y);
        continue;
      }
      if (layout.fourBytesToAColumn) {
        int at = layout.pixelsAt(y, x), above = layout.secondByteAt(y, x);
        byte[] other = banks.beside().bytes;
        plotColours(x, y, 0, other[at]);
        plotColours(x, y, 1, screen[at]);
        plotColours(x, y, 2, other[above]);
        plotColours(x, y, 3, screen[above]);
        continue;
      }
      if (layout.twoBytesToAColumn) {
        byte pair = layout.pairOfColours;
        int wide = ((screen[layout.pixelsAt(y, x)] & 0xff) << 8) | (screen[layout.secondByteAt(y, x)] & 0xff);
        canvas.plot16(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, wide,
            colouring.ink(pair), colouring.paper(pair));
        continue;
      }
      byte attribute = screen[layout.colourAt(y, x)];
      canvas.plot8(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, screen[layout.pixelsAt(y, x)],
          colouring.ink(attribute), colouring.paper(attribute));
    }
  }
}

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
 * The picture as far as it has been put on the canvas, and putting more of it there.
 * <p>
 * A frame is not painted at the end of itself: the beam decides a cell when it reaches it, so what
 * is plotted is plotted from the memory as it stands at that moment, and a byte written after that
 * belongs to the next frame. That is what keeps how far it has got - a cell, not a pixel - and
 * only cells that something marked are touched.
 * <p>
 * Where the beam is is not its business; it is told how far to go.
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

  /** Whether the beam has already gone past that cell this frame. */
  public boolean past(int x, int y) {
    return y > plottedY || (y == plottedY && x >= plottedX);
  }

  /** The beam has got this far: everything up to here is decided now, from the memory as it is. */
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

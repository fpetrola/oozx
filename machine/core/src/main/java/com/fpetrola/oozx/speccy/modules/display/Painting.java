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
  }

  /** A bitmap byte and the two colours of its cell, which is how every Sinclair draws. */
  public final Line sinclair = this::plotSinclair;

  /**
   * Two bytes of the same line from the two display files, sixteen pixels wide in the one pair of
   * colours the whole picture is drawn in, with no attribute read at all.
   */
  public final Line twoBytesToAColumn = this::plotTwoBytes;

  /** Four bytes from two banks at once, each one two colours, so eight pixels are eight colours. */
  public final Line fourBytesToAColumn = this::plotFourBytes;

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

  public void startAgain() {
    plottedX = plottedY = 0;
    if (line.allOfItEveryFrame()) dirty.all();
  }

  /** A byte that is not a bitmap but two colours: the same bits an attribute puts its ink and paper in. */
  private void plotColours(int x, int y, int pair, byte colours) {
    canvas.plotPair(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, pair,
        Colouring.inkBits(colours), Colouring.paperBits(colours));
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

  private void plotTwoBytes(int y, int bits) {
    byte[] screen = banks.shown().bytes;
    byte pair = layout.pairOfColours;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      int wide = ((screen[layout.pixelsAt(y, x)] & 0xff) << 8) | (screen[layout.secondByteAt(y, x)] & 0xff);
      canvas.plot16(x + BORDER_WIDTH_COLS, y + BORDER_HEIGHT, wide, colouring.ink(pair), colouring.paper(pair));
    }
  }

  private void plotFourBytes(int y, int bits) {
    byte[] screen = banks.shown().bytes, other = banks.beside().bytes;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      int at = layout.pixelsAt(y, x), above = layout.secondByteAt(y, x);
      plotColours(x, y, 0, other[at]);
      plotColours(x, y, 1, screen[at]);
      plotColours(x, y, 2, other[above]);
      plotColours(x, y, 3, screen[above]);
    }
  }

  private void plotLine(int y, int from, int to) {
    int bits = dirty.between(y, from, to);
    if (bits == 0) {
      return;
    }
    dirty.plotted(y, bits);
    line.paint(y, bits);
  }
}

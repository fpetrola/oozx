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

package model.tests.display;

import com.fpetrola.oozx.speccy.modules.display.Border;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The border is one colour at a time and the beam is somewhere when it changes, so what shows at a
 * point is the last colour set before the beam got there and not the colour at the end of the
 * frame. That is what paints stripes. From prototypes/tdd/display.
 */
class BorderTest {
  private final Picture canvas = new Picture();
  private int column, row;
  private final Border border = new Border(canvas, new com.fpetrola.oozx.speccy.modules.display.Colouring(), new Border.BeamAt() {
    public int column() {
      return column;
    }

    public int row() {
      return row;
    }
  });

  private void beamAt(int column, int row) {
    this.column = column;
    this.row = row;
  }

  /** A row of the top border, which is border all the way across. */
  private int colourOfRow(int row, int column) {
    return canvas.pixels[row * Picture.STRIDE + column * 8];
  }

  @Test
  void oneColourForAWholeFrameFillsTheWholeBorder() {
    border.becomes(2);
    border.paintTheFrame();

    assertEquals(Picture.SINCLAIR[2], colourOfRow(0, 0));
    assertEquals(Picture.SINCLAIR[2], colourOfRow(5, 10), "top border");
    assertEquals(Picture.SINCLAIR[2], colourOfRow(Display.SCREEN_HEIGHT - 1, 0), "and bottom");
  }

  @Test
  void aChangeMidFrameShowsFromWhereTheBeamWasAndNotBefore() {
    border.becomes(1);
    beamAt(10, 5);
    border.becomes(6);
    border.paintTheFrame();

    assertEquals(Picture.SINCLAIR[1], colourOfRow(4, 20), "the row above the change is all the first colour");
    assertEquals(Picture.SINCLAIR[1], colourOfRow(5, 9), "and so is this row up to where the beam was");
    assertEquals(Picture.SINCLAIR[6], colourOfRow(5, 10), "from there on it is the second");
    assertEquals(Picture.SINCLAIR[6], colourOfRow(6, 0), "and every row after it");
  }

  @Test
  void severalChangesInOneFramePaintBands() {
    border.becomes(1);
    beamAt(0, 4);
    border.becomes(2);
    beamAt(0, 8);
    border.becomes(3);
    border.paintTheFrame();

    assertEquals(Picture.SINCLAIR[1], colourOfRow(2, 0));
    assertEquals(Picture.SINCLAIR[2], colourOfRow(6, 0));
    assertEquals(Picture.SINCLAIR[3], colourOfRow(10, 0));
  }

  @Test
  void aFrameStartsWithTheColourTheLastOneEndedOn() {
    border.becomes(5);
    border.paintTheFrame();

    border.refreshAll();
    border.paintTheFrame();
    assertEquals(Picture.SINCLAIR[5], colourOfRow(3, 0), "nothing was written this frame, so it is what it was");
  }
}

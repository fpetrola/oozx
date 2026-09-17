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

import com.fpetrola.oozx.speccy.modules.display.*;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Who paints a column of the screen. Three rules read the bytes the machine is showing, and a
 * fourth party can sit in front of them when its pixels are not there to be read.
 */
class PaintingTest {
  private static final int LINE = 40, COLUMN = 3;
  private static final int COLUMNS = Display.SCREEN_WIDTH_COLS - 2 * Display.BORDER_WIDTH_COLS;
  private static final int LAST_LINE = Display.HEIGHT - 1;

  private final SpectrumMemory banks = new SpectrumMemory(new Rom.Protection());
  private final ScreenLayout layout = new ScreenLayout();
  private final DirtyCells dirty = new DirtyCells(Display.HEIGHT);
  private final Colouring colouring = new Colouring();
  private final Picture canvas = new Picture();
  private final Painting painting = new Painting(banks, layout, dirty, colouring, canvas);

  PaintingTest() {
    banks.show(banks.ram(5), offset -> dirty.cell(layout.columnOf(offset), layout.lineOf(offset)));
    banks.ram(5).bytes[layout.pixelsAt(LINE, COLUMN)] = (byte) 0xff;
    banks.ram(5).bytes[layout.colourAt(LINE, COLUMN)] = 7;
  }

  private int leftmostPixel() {
    return canvas.pixels[(LINE + Display.BORDER_HEIGHT) * Picture.STRIDE + (COLUMN + Display.BORDER_WIDTH_COLS) * 8];
  }

  private static void forEachColumn(int bits, java.util.function.IntConsumer column) {
    for (; bits != 0; bits &= bits - 1) column.accept(Integer.numberOfTrailingZeros(bits));
  }

  private void paint() {
    dirty.all();
    painting.startAgain();
    painting.upTo(COLUMNS, LAST_LINE);
  }

  @Test
  void withNobodySittingTheMachinePaintsItsOwnBytes() {
    paint();

    assertEquals(Picture.SINCLAIR[7], leftmostPixel(), "the byte is all ink and the attribute says white");
  }

  @Test
  void whoeverSaysThePixelsAreTheirsPaintsThemInstead() {
    List<String> painted = new ArrayList<>();
    painting.line((y, bits) -> forEachColumn(bits, x -> {
      painted.add(x + "," + y);
      canvas.plot8(x + Display.BORDER_WIDTH_COLS, y + Display.BORDER_HEIGHT, (byte) 0xff, (byte) 2, (byte) 0);
    }));
    paint();

    assertEquals(Picture.SINCLAIR[2], leftmostPixel(), "the colour is the one it painted, not the attribute's");
    assertEquals(COLUMNS, painted.stream().filter(c -> c.endsWith("," + LINE)).count(),
        "and it was asked for every column of the line, not only the one with a byte in it");
  }

  @Test
  void theSeatIsGivenUpAndTheThreeRulesAreBackAsTheyWere() {
    painting.line((y, bits) -> forEachColumn(bits, x ->
        canvas.plot8(x + Display.BORDER_WIDTH_COLS, y + Display.BORDER_HEIGHT, (byte) 0xff, (byte) 2, (byte) 0)));
    paint();
    painting.line(null);
    paint();

    assertEquals(Picture.SINCLAIR[7], leftmostPixel());
  }

  /**
   * The whole point of the dirty cells is that the picture is made of the machine's bytes, so a
   * cell nobody wrote to cannot have changed. Whoever has pixels of its own breaks that: they
   * change for reasons written nowhere in this memory, and a cell nobody wrote to would keep what
   * it had for as long as the game ran.
   */
  @Test
  void whoeverHasThePixelsIsAskedForEveryColumnOfEveryFrame() {
    List<String> painted = new ArrayList<>();
    painting.line(new Painting.Line() {
      public void paint(int y, int bits) {
        forEachColumn(bits, x -> painted.add(x + "," + y));
      }

      public boolean allOfItEveryFrame() {
        return true;
      }
    });
    paint();
    painted.clear();

    painting.startAgain();
    painting.upTo(COLUMNS, LAST_LINE);

    assertEquals(COLUMNS * Display.HEIGHT, painted.size(), "every cell of the screen, with nothing written to any of them");
  }

  @Test
  void andWithNobodySittingACellNobodyWroteToIsNobodysToPaint() {
    paint();
    dirty.plotted(LINE, dirty.between(LINE, 0, COLUMNS));

    painting.startAgain();
    painting.upTo(COLUMNS, LAST_LINE);

    assertEquals(0, dirty.between(LINE, 0, COLUMNS), "which is what makes the usual screen cheap");
  }

}

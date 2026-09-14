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
    banks.show(banks.ram(5), offset -> {
    });
    banks.ram(5).bytes[layout.pixelsAt(LINE, COLUMN)] = (byte) 0xff;
    banks.ram(5).bytes[layout.colourAt(LINE, COLUMN)] = 7;
  }

  private int leftmostPixel() {
    return canvas.pixels[(LINE + Display.BORDER_HEIGHT) * Picture.STRIDE + (COLUMN + Display.BORDER_WIDTH_COLS) * 8];
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
    painting.pixelsOfItsOwn((x, y) -> {
      painted.add(x + "," + y);
      canvas.plot8(x + Display.BORDER_WIDTH_COLS, y + Display.BORDER_HEIGHT, (byte) 0xff, (byte) 2, (byte) 0);
    });
    paint();

    assertEquals(Picture.SINCLAIR[2], leftmostPixel(), "the colour is the one it painted, not the attribute's");
    assertEquals(COLUMNS, painted.stream().filter(c -> c.endsWith("," + LINE)).count(),
        "and it was asked for every column of the line, not only the one with a byte in it");
  }

  @Test
  void theSeatIsGivenUpAndTheThreeRulesAreBackAsTheyWere() {
    painting.pixelsOfItsOwn((x, y) -> canvas.plot8(x + Display.BORDER_WIDTH_COLS, y + Display.BORDER_HEIGHT, (byte) 0xff, (byte) 2, (byte) 0));
    paint();
    painting.pixelsOfItsOwn(null);
    paint();

    assertEquals(Picture.SINCLAIR[7], leftmostPixel());
  }

  @Test
  void aColumnThatIsNotDirtyIsNobodysToPaint() {
    List<String> painted = new ArrayList<>();
    painting.pixelsOfItsOwn((x, y) -> painted.add(x + "," + y));
    paint();
    painted.clear();

    painting.startAgain();
    dirty.cell(COLUMN, LINE);
    painting.upTo(COLUMNS, LAST_LINE);

    assertEquals(List.of(COLUMN + "," + LINE), painted, "the beam went past every line and one cell was dirty");
  }
}

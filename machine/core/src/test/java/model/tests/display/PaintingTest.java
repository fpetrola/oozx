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

  /** With nothing being shown the screen is not painted, which is asked once a line and not once a cell. */
  @Test
  void aPictureNobodyIsLookingAtIsNotPainted() {
    canvas.active = false;
    paint();

    assertEquals(0, leftmostPixel());
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

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

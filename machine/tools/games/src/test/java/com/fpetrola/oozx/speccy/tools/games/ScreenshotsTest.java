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

package com.fpetrola.oozx.speccy.tools.games;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screenshots follow the row's width instead of fixing it.
 * <p>
 * This is what stopped the browser resizing with its contents: an icon in a label reports the
 * pixels it happens to have, so a wider window only moved empty space around. Passing no URLs
 * keeps the test off the network — the shape a Spectrum screen has is enough to measure with,
 * and it is what the component reserves before any picture arrives.
 */
public class ScreenshotsTest {

  private Screenshots inRowOfWidth(int width) {
    JPanel row = new JPanel();
    Screenshots shots = new Screenshots(null, null, null);
    row.add(shots);
    row.setSize(width, 400);
    return shots;
  }

  @Test
  public void theScreenshotsAreAsWideAsTheRowTheySitIn() {
    assertEquals(600, inRowOfWidth(600).getPreferredSize().width);
    assertEquals(1200, inRowOfWidth(1200).getPreferredSize().width);
  }

  @Test
  public void theyGetTallerAsTheyGetWider() {
    Dimension narrow = inRowOfWidth(600).getPreferredSize();
    Dimension wide = inRowOfWidth(1200).getPreferredSize();

    assertTrue(wide.height > narrow.height,
        "a wider row should make the screenshots taller, not just leave more empty space; "
            + narrow.height + " -> " + wide.height);

    // Two screens side by side with a gap, each keeping the 256x192 shape.
    assertEquals((600 - 10) / 2 * 192 / 256, narrow.height);
    assertEquals((1200 - 10) / 2 * 192 / 256, wide.height);
  }

  @Test
  public void withNoRowToMeasureItAsksForTwoScreensSideBySide() {
    assertEquals(256 * 2 + 10, new Screenshots(null, null, null).getPreferredSize().width);
  }
}

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

package model.tests;

import com.fpetrola.oozx.speccy.peripherals.t.ScreenshotPair;
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
public class ScreenshotPairTest {

  private ScreenshotPair inRowOfWidth(int width) {
    JPanel row = new JPanel();
    ScreenshotPair shots = new ScreenshotPair(null, null, null);
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
    assertEquals(256 * 2 + 10, new ScreenshotPair(null, null, null).getPreferredSize().width);
  }
}

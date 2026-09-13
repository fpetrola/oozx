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

package model.tests.ui;

import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import com.fpetrola.oozx.speccy.screen.SpeccyScreen;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A frame whose rows sit further apart than the picture is wide, which is what a machine that can
 * draw a column of sixteen pixels leaves behind. Reading it as if they touched shows the far half
 * of each row as a row of its own, so the picture arrives with twice the lines and half of them
 * are somebody else's.
 */
class ScreenRowsComeFromTheFramesRowsTest {
  private static final int WIDTH = 320, HEIGHT = 240, STRIDE = 640;
  private static final int SHOWN = 0xB20000, BEYOND = 0x00B200;

  /** Every row is one colour as far as the picture goes, and another colour past its right edge. */
  private static int[] aFrameWithSomethingBeyondItsRightEdge() {
    int[] pixels = new int[STRIDE * HEIGHT];
    for (int row = 0; row < HEIGHT; row++) {
      for (int x = 0; x < STRIDE; x++) {
        pixels[row * STRIDE + x] = x < WIDTH ? SHOWN : BEYOND;
      }
    }
    return pixels;
  }

  private static BufferedImage painted(boolean border) {
    Map<String, String> previous = ScreenSettings.getDefaults();
    Map<String, String> asked = new LinkedHashMap<>(previous);
    asked.put("border", Boolean.toString(border));
    ScreenSettings.setDefaults(asked);
    try {
      SpeccyScreen screen = new SpeccyScreen(aFrameWithSomethingBeyondItsRightEdge(), STRIDE);
      Dimension size = screen.getPreferredSize();
      screen.setSize(size);
      BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
      Graphics2D pen = image.createGraphics();
      screen.paint(pen);
      pen.dispose();
      return image;
    } finally {
      ScreenSettings.setDefaults(previous);
    }
  }

  @Test
  void whatIsPastTheRightEdgeIsNeverDrawnAsALineOfItsOwn() {
    BufferedImage withBorder = painted(true);
    for (int row = 0; row < withBorder.getHeight(); row++) {
      assertEquals(SHOWN, withBorder.getRGB(0, row) & 0xffffff, "row " + row + " of the picture");
    }

    BufferedImage cropped = painted(false);
    for (int row = 0; row < cropped.getHeight(); row++) {
      assertEquals(SHOWN, cropped.getRGB(0, row) & 0xffffff, "row " + row + " of the cropped picture");
    }
  }
}

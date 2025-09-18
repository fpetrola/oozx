/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package model.tests.ui;

import com.fpetrola.oozx.speccy.screen.SpeccyScreen;
import com.fpetrola.oozx.speccy.screen.ScreenSettings;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The size the screen asks for is what a window packed around it gets, so it has to be the
 * picture it shows: 320 by 236 with the border, 256 by 192 without. It used to ask
 * for the bare picture whatever the defaults said about the border.
 */
class SpeccyScreenSizeTest {
  @Test
  void itPrefersThePictureItShowsAtItsZoom() {
    Map<String, String> previous = ScreenSettings.getDefaults();
    Map<String, String> withBorder = new LinkedHashMap<>(previous);
    withBorder.put("border", "true");
    ScreenSettings.setDefaults(withBorder);
    try {
      SpeccyScreen screen = new SpeccyScreen(new int[320 * 240]);
      assertEquals(new Dimension(320, 236), screen.getPreferredSize(), "with the border");
      screen.setBorderVisible(false);
      assertEquals(new Dimension(256, 192), screen.getPreferredSize(), "without the border");
    } finally {
      ScreenSettings.setDefaults(previous);
    }
  }
  /**
   * At a whole zoom the picture fills the panel. It used to be drawn one multiple short - an exact
   * 2x window showed a 1x picture adrift in a wide margin - because the floor was written as a
   * ceiling minus one, which is a multiple too few on the whole numbers themselves.
   */
  @Test
  void itDrawsAnExactMultipleAtThatMultiple() {
    int[] allRed = new int[320 * 240];
    java.util.Arrays.fill(allRed, 0xB20000);
    SpeccyScreen screen = new SpeccyScreen(allRed);
    screen.setZoom(2);
    Dimension asked = screen.getPreferredSize();
    screen.setSize(asked);
    BufferedImage painted = new BufferedImage(asked.width, asked.height, BufferedImage.TYPE_INT_RGB);
    Graphics2D pen = painted.createGraphics();
    screen.paint(pen);
    pen.dispose();

    assertEquals(painted.getRGB(asked.width / 2, asked.height / 2), painted.getRGB(2, 2),
        "the corner of a 2x window is not the picture, so it was drawn smaller and centred");
  }
}

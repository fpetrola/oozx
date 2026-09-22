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

package com.fpetrola.oozx.speccy.tools.curvature;

import com.fpetrola.oozx.speccy.screen.Knob;
import com.fpetrola.oozx.speccy.screen.ScreenContext;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a picture painted on the inside of a bottle does: the corners go, the middle stays. */
class TheGlassBulgesTest {

  /** A white screen with a black grid, which is where a bend is visible at all. */
  private static BufferedImage lined() {
    BufferedImage picture = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < picture.getHeight(); y++) {
      for (int x = 0; x < picture.getWidth(); x++) {
        picture.setRGB(x, y, x % 20 == 0 || y % 15 == 0 ? 0x000000 : 0xFFFFFF);
      }
    }
    return picture;
  }

  private static TubeCurve curvedBy(double amount) {
    TubeCurve curve = new TubeCurve();
    curve.knobs().get(0).set(amount);
    return curve;
  }

  @Test
  void flatItDoesNothingAndSaysSo() {
    TubeCurve flat = curvedBy(0);

    assertTrue(flat.isTransparent(), "a flat panel is not an effect and should be skipped");
  }

  @Test
  void bulgingItTakesTheCornersAndKeepsTheMiddle() {
    BufferedImage picture = lined();
    TubeCurve curve = curvedBy(8);

    BufferedImage curved = curve.apply(picture, new ScreenContext());

    assertFalse(curve.isTransparent(), "it was set to bulge");
    assertEquals(Color.BLACK.getRGB(), curved.getRGB(0, 0), "the corner is outside the glass");
    assertEquals(picture.getRGB(100, 75), curved.getRGB(100, 75),
        "the middle is where the glass is nearest and nothing moves");
  }

  /** The point of a bend: what was a straight line is not one any more. */
  @Test
  void aStraightLineComesOutBent() {
    BufferedImage picture = lined();
    BufferedImage curved = curvedBy(8).apply(picture, new ScreenContext());

    int middleOfTheTopLine = rowOfTheFirstDarkPixel(curved, 100);
    int nearTheSide = rowOfTheFirstDarkPixel(curved, 20);

    assertNotEquals(middleOfTheTopLine, nearTheSide,
        "the top line came out at the same height in the middle and at the side: it is not bent");
  }

  /** How far down that column the picture starts, which is where the glass has curved away. */
  private static int rowOfTheFirstDarkPixel(BufferedImage picture, int column) {
    for (int row = 0; row < picture.getHeight(); row++) {
      if ((picture.getRGB(column, row) & 0xFFFFFF) != 0) {
        return row;
      }
    }
    return picture.getHeight();
  }

  @Test
  void itBringsTheKnobThatConfiguresIt() {
    Knob knob = new TubeCurve().knobs().get(0);

    assertEquals("curve", knob.key());
    assertEquals(Knob.Kind.NUMBER, knob.kind());
    assertEquals("Television", knob.group(), "it belongs beside the rest of the tube");
  }
}

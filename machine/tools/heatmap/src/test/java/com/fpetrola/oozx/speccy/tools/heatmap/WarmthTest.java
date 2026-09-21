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


package com.fpetrola.oozx.speccy.tools.heatmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the picture makes of a count: the part of the window that can be wrong quietly. */
class WarmthTest {

  private static int brightness(int colour) {
    return (colour >> 16 & 0xff) + (colour >> 8 & 0xff) + (colour & 0xff);
  }

  @Test
  void whatNeverRanIsNotOnTheMap() {
    assertEquals(0x101014, HeatmapFrame.warmth(0, 1000), "cold, and the same cold everywhere");
  }

  @Test
  void theMoreItRanTheBrighterItIs() {
    int once = HeatmapFrame.warmth(1, 10_000);
    int often = HeatmapFrame.warmth(100, 10_000);
    int always = HeatmapFrame.warmth(10_000, 10_000);

    assertTrue(brightness(once) < brightness(often), "a hundred times is not one time");
    assertTrue(brightness(often) < brightness(always), "and the inner loop is the brightest");
  }

  /**
   * The reason for the log scale: on a straight one a place that ran once against an inner loop
   * that ran a million times rounds to black, and finding the code that runs once is most of
   * what this is for - that is where the setup, the level loader and the death routine live.
   */
  @Test
  void whatRanOnlyOnceIsStillVisibleBesideAMillion() {
    int once = HeatmapFrame.warmth(1, 1_000_000);

    assertTrue(brightness(once) > 20, "it rounded away to nothing: " + Integer.toHexString(once));
  }

  @Test
  void theHottestIsAsBrightAsItGoes() {
    assertTrue(brightness(HeatmapFrame.warmth(500, 500)) > 600, "the top of the scale is white");
  }
}

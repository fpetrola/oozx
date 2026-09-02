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

import com.fpetrola.oozx.speccy.TvScreen;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the leads are for, held to.
 * <p>
 * The point of the thing is not that it blurs - anything blurs - but that it blurs COLOUR harder
 * than BRIGHTNESS, and harder the worse the lead. That is the one property a change here could
 * quietly lose while still looking like something, so it is the one measured.
 */
class TvScreenTest {

  /** A Spectrum screen's worth of hard edges and flat colour, which is what these filters meet. */
  private static BufferedImage picture() {
    BufferedImage image = new BufferedImage(320, 236, BufferedImage.TYPE_INT_RGB);
    int[] palette = {0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7};
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        // Blocks of eight, the way attributes fall, with a dithered seam down the middle of each:
        // fine detail is where a composite lead shows what it does.
        int block = (x / 8 + y / 8 * 3) % palette.length;
        boolean dither = (x % 8) >= 4 && ((x + y) & 1) == 0;
        image.setRGB(x, y, dither ? palette[(block + 3) % palette.length] : palette[block]);
      }
    }
    return image;
  }

  private static BufferedImage through(TvScreen screen) {
    BufferedImage image = picture();
    screen.apply(image, new TvScreen.Scratch());
    return image;
  }

  /** Mean change from one pixel to the next along a line, in brightness and in colour. */
  private static double[] detail(BufferedImage image) {
    double luma = 0, chroma = 0;
    long counted = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      double[] previous = yuv(image.getRGB(0, y));
      for (int x = 1; x < image.getWidth(); x++) {
        double[] here = yuv(image.getRGB(x, y));
        luma += Math.abs(here[0] - previous[0]);
        chroma += Math.abs(here[1] - previous[1]) + Math.abs(here[2] - previous[2]);
        previous = here;
        counted++;
      }
    }
    return new double[]{luma / counted, chroma / counted};
  }

  private static double[] yuv(int rgb) {
    double r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
    double y = 0.299 * r + 0.587 * g + 0.114 * b;
    return new double[]{y, 0.492 * (b - y), 0.877 * (r - y)};
  }

  @Test
  void aMonitorShowsWhatTheMachineDrew() {
    BufferedImage untouched = picture();
    BufferedImage through = through(TvScreen.RGB_MONITOR);
    for (int y = 0; y < untouched.getHeight(); y++) {
      for (int x = 0; x < untouched.getWidth(); x++) {
        assertEquals(untouched.getRGB(x, y), through.getRGB(x, y),
            "a monitor put something between the machine and the screen at " + x + "," + y);
      }
    }
  }

  @Test
  void eachLeadCostsMoreThanTheOneBefore() {
    double[] monitor = detail(through(TvScreen.RGB_MONITOR));
    double[] scart = detail(through(TvScreen.SCART));
    double[] composite = detail(through(TvScreen.COMPOSITE));
    double[] aerial = detail(through(TvScreen.AERIAL));

    assertTrue(scart[1] < monitor[1], "a scart lead should lose colour a monitor keeps");
    assertTrue(composite[1] < scart[1], "a composite lead should lose more colour than a scart one");
    assertTrue(aerial[1] < composite[1], "an aerial should lose more colour than a composite lead");

    assertTrue(scart[0] < monitor[0], "a scart lead should soften what a monitor keeps sharp");
    assertTrue(composite[0] < scart[0], "a composite lead should be softer than a scart one");
    assertTrue(aerial[0] < composite[0], "an aerial should be softer than a composite lead");
  }

  @Test
  void colourLosesMoreThanBrightnessDoes() {
    // The physics being imitated: colour rides a subcarrier with a fraction of the bandwidth, so
    // it is colour that smears. A filter that softened the picture evenly would pass the test
    // above and fail this one, and would not look like a television.
    double[] monitor = detail(through(TvScreen.RGB_MONITOR));
    for (TvScreen lead : new TvScreen[]{TvScreen.SCART, TvScreen.COMPOSITE, TvScreen.AERIAL}) {
      double[] through = detail(through(lead));
      double colourKept = through[1] / monitor[1];
      double brightnessKept = through[0] / monitor[0];
      assertTrue(colourKept < brightnessKept,
          lead + " kept " + colourKept + " of its colour and " + brightnessKept
              + " of its brightness, and colour is the one that should go first");
    }
  }

  @Test
  void theScratchIsReusedAcrossSizes() {
    // One frame's room, kept between frames, and the picture changes size when the border is
    // turned on and off. A scratch that only ever grew would be fine; one that assumed a size
    // would be a stripe of somebody else's line down the side of the screen.
    TvScreen.Scratch scratch = new TvScreen.Scratch();
    BufferedImage wide = picture();
    TvScreen.COMPOSITE.apply(wide, scratch);

    BufferedImage narrow = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    narrow.getGraphics().drawImage(picture(), 0, 0, null);
    BufferedImage alone = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    alone.getGraphics().drawImage(picture(), 0, 0, null);

    TvScreen.COMPOSITE.apply(narrow, scratch);
    TvScreen.COMPOSITE.apply(alone, new TvScreen.Scratch());

    int[] shared = narrow.getRGB(0, 0, 256, 192, null, 0, 256);
    int[] fresh = alone.getRGB(0, 0, 256, 192, null, 0, 256);
    assertArrayEquals(fresh, shared, "a reused scratch changed the picture");
  }

  @Test
  void aLeadIsFoundByEitherNameAndNeverThrows() {
    assertEquals(TvScreen.COMPOSITE, TvScreen.byName("COMPOSITE"));
    assertEquals(TvScreen.COMPOSITE, TvScreen.byName("Composite Video"));
    assertEquals(TvScreen.AERIAL, TvScreen.byName("aerial (rf)"));
    // A name out of a settings file written by a version that had other names, which is a thing
    // that happens, and is not worth a picture nobody can see.
    assertEquals(TvScreen.RGB_MONITOR, TvScreen.byName("s-video"));
    assertEquals(TvScreen.RGB_MONITOR, TvScreen.byName(""));
  }
}

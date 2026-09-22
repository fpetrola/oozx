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
import com.fpetrola.oozx.speccy.screen.ScreenEffect;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.List;

/**
 * The curve of the tube: the picture is painted on the inside of a glass bottle, so the middle
 * is nearer than the corners and straight lines are not.
 * <p>
 * Everything else here works on the colours - what the phosphor does to them, what the lead does
 * to them, what the mask leaves of them. This is the one thing about a television that is not
 * about colour at all, which is why a screen with all of those on still reads as a flat picture
 * with an effect over it rather than as a tube.
 * <p>
 * It runs on the screen and not on the picture, because it bends the windowful: bending 256 by
 * 192 and then magnifying it would magnify the bend with it.
 * <p>
 * It is also the example of what arrives in a jar: nothing here is named anywhere in the
 * emulator, it is found by answering to {@link ScreenEffect}, and the knob it brings is how it
 * is configured, written down and remembered.
 */
public class TubeCurve implements ScreenEffect {

  /** How much the glass bulges, as hundredths: 0 is a flat panel, 10 is a small old portable. */
  private double curve;

  /**
   * Where each pixel of the window reads from, worked out once for a size and kept.
   * <p>
   * A bend is the same arithmetic for every frame, and there are fifty a second: what changes is
   * the picture, not where each pixel of it comes from. Trigonometry per pixel per frame was
   * three milliseconds of a twenty millisecond frame; a table lookup is a memory read.
   */
  private int[] readsFrom = new int[0];
  private int forWidth;
  private int forHeight;
  private double forCurve;

  /**
   * The window this is drawn into, kept between frames along with its own pixels.
   * <p>
   * A new image a frame is a megabyte and a half of rubbish fifty times a second, and reading
   * and writing it through getRGB and setRGB converts every pixel twice: that was ten
   * milliseconds of a twenty millisecond frame, all of it in the copying and none in the bend.
   */
  private BufferedImage curved;
  private int[] curvedPixels;

  @Override
  public String label() {
    return "Tube curve";
  }

  /** After the scaler: it bends the windowful, not the 256 by 192 the machine drew. */
  @Override
  public When when() {
    return When.ON_THE_SCREEN;
  }

  @Override
  public boolean isTransparent() {
    return curve <= 0;
  }

  @Override
  public List<Knob> knobs() {
    return List.of(Knob.number("curve", "Tube curve",
        "How much the glass bulges. A flat panel is nothing; a small portable from the eighties "
            + "is most of the way along.",
        "Television", 0, 10, 0.5, 0,
        () -> curve, value -> curve = ((Number) value).doubleValue()));
  }

  @Override
  public BufferedImage apply(BufferedImage picture, ScreenContext context) {
    int width = picture.getWidth();
    int height = picture.getHeight();
    if (width < 2 || height < 2) {
      return picture;
    }
    int[] from = mapFor(width, height);
    int[] pixels = pixelsOf(picture);
    int[] bent = into(width, height);
    for (int at = 0; at < bent.length; at++) {
      int reads = from[at];
      bent[at] = reads < 0 ? 0 : pixels[reads];
    }
    return curved;
  }

  /** The window to draw into, made once for a size and written into from then on. */
  private int[] into(int width, int height) {
    if (curved == null || curved.getWidth() != width || curved.getHeight() != height) {
      curved = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      curvedPixels = ((DataBufferInt) curved.getRaster().getDataBuffer()).getData();
    }
    return curvedPixels;
  }

  /** The picture's own pixels when they are there to be had, rather than a copy of them. */
  private static int[] pixelsOf(BufferedImage picture) {
    if (picture.getRaster().getDataBuffer() instanceof DataBufferInt ints
        && (picture.getType() == BufferedImage.TYPE_INT_RGB
        || picture.getType() == BufferedImage.TYPE_INT_ARGB)) {
      return ints.getData();
    }
    return picture.getRGB(0, 0, picture.getWidth(), picture.getHeight(),
        null, 0, picture.getWidth());
  }

  /**
   * Which pixel each one of the window reads from, or -1 for the black outside the glass.
   * <p>
   * The bend is the usual one: how far a point is pushed out depends on how far it already is
   * from the middle the other way, which is what makes the edges bow and the corners the most.
   */
  private int[] mapFor(int width, int height) {
    if (width == forWidth && height == forHeight && curve == forCurve) {
      return readsFrom;
    }
    double bulge = curve / 100;
    int[] map = new int[width * height];
    for (int y = 0; y < height; y++) {
      double downwards = (2.0 * y / (height - 1)) - 1;
      for (int x = 0; x < width; x++) {
        double across = (2.0 * x / (width - 1)) - 1;
        double sourceX = across * (1 + bulge * downwards * downwards);
        double sourceY = downwards * (1 + bulge * across * across);
        int readX = (int) Math.round((sourceX + 1) * (width - 1) / 2);
        int readY = (int) Math.round((sourceY + 1) * (height - 1) / 2);
        map[y * width + x] = readX < 0 || readY < 0 || readX >= width || readY >= height
            ? -1 : readY * width + readX;
      }
    }
    readsFrom = map;
    forWidth = width;
    forHeight = height;
    forCurve = curve;
    return map;
  }
}

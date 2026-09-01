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

package com.fpetrola.oozx.speccy.screen;

import java.awt.image.BufferedImage;

/**
 * The effects that are not scalers: what a tube did to the picture, and what someone wants done
 * to it for their own eyes.
 * <p>
 * Two families, and it matters which side of the scaler each runs on. A shadow mask and scan
 * lines are features of the SCREEN - they are the same size whatever the picture is magnified to,
 * so they go on afterwards. A tint and the persistence of the phosphor are features of the
 * PICTURE, and cost a fraction as much on 256 by 192 as on a window, so they go on before.
 */
public final class ScreenEffects {

  private ScreenEffects() {
  }

  /**
   * The unlit gaps a tube left between its lines.
   * <p>
   * On the scaled picture, because a scan line is a line of the SCREEN: drawn before scaling it
   * would come out a band as wide as the magnification.
   * <p>
   * BY WHERE A ROW FALLS INSIDE A MACHINE LINE, not by whether its number is odd. Darkening
   * every other row only means anything at exactly twice size; at three times it darkens a third
   * of each line and two thirds of the next, and below twice there is no other row to darken, so
   * that version did nothing at all below 2x and something different at every size above it. Two
   * windows of different sizes showing the same setting looked like two different settings, which
   * is what sent someone looking for a bug in how the setting was saved.
   * <p>
   * So: each machine line is lit brightest down its middle and falls away to its edges, which is
   * what a beam does, and holds at any magnification. There is still nothing to draw at life
   * size - a row IS a line, and the whole picture would just go dim - so it fades in over the
   * first doubling rather than appearing all at once.
   */
  public static class Scanlines implements ScreenEffect {
    private final double depth;
    private final int sourceHeight;

    public Scanlines(double depth, int sourceHeight) {
      this.depth = depth;
      this.sourceHeight = sourceHeight;
    }

    public String label() {
      return "Scan lines";
    }

    public boolean isTransparent() {
      return depth <= 0;
    }

    public BufferedImage apply(BufferedImage picture, ScreenContext context) {
      int height = picture.getHeight(), width = picture.getWidth();
      double magnification = height / (double) sourceHeight;
      double strength = depth * Math.max(0, Math.min(1, magnification - 1));
      if (strength <= 0) {
        return picture;
      }
      int[] pixels = ScreenContext.pixelsOf(picture);
      for (int y = 0; y < height; y++) {
        double into = (y * (double) sourceHeight / height) % 1;
        // 0 down the middle of a machine line, 1 at the seam between two of them.
        double fromMiddle = Math.abs(2 * into - 1);
        int keep = (int) ((1 - strength * fromMiddle) * 100);
        if (keep >= 100) {
          continue;
        }
        int row = y * width;
        for (int x = 0; x < width; x++) {
          pixels[row + x] = scaleRgb(pixels[row + x], keep);
        }
      }
      return picture;
    }
  }

  /**
   * The grille or the mask the phosphors were laid out behind.
   * <p>
   * A colour tube does not have pixels; it has stripes or dots of red, green and blue phosphor,
   * and the picture is a beam landing across them. Dimming each screen column in turn by colour
   * is the cheap way to suggest it, and at three screen pixels to a machine pixel it is roughly
   * what one stripe was worth.
   */
  public static class ShadowMask implements ScreenEffect {
    /** Which pattern the phosphors were in. */
    public enum Pattern { NONE, APERTURE_GRILLE, SLOT_MASK }

    private final Pattern pattern;
    private final double depth;

    public ShadowMask(Pattern pattern, double depth) {
      this.pattern = pattern;
      this.depth = depth;
    }

    public String label() {
      return "Shadow mask";
    }

    public boolean isTransparent() {
      return pattern == Pattern.NONE || depth <= 0;
    }

    public BufferedImage apply(BufferedImage picture, ScreenContext context) {
      int keep = (int) ((1 - depth) * 100);
      int[] pixels = ScreenContext.pixelsOf(picture);
      int width = picture.getWidth(), height = picture.getHeight();
      for (int y = 0; y < height; y++) {
        // A grille runs the whole height; a slot mask is offset every other pair of rows, which
        // is what makes the second look like dots rather than stripes.
        int shift = pattern == Pattern.SLOT_MASK && (y / 2) % 2 == 1 ? 1 : 0;
        int row = y * width;
        for (int x = 0; x < width; x++) {
          int rgb = pixels[row + x];
          int stripe = (x + shift) % 3;
          int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
          if (stripe != 0) r = r * keep / 100;
          if (stripe != 1) g = g * keep / 100;
          if (stripe != 2) b = b * keep / 100;
          pixels[row + x] = 0xFF000000 | r << 16 | g << 8 | b;
        }
      }
      return picture;
    }
  }

  /**
   * What the phosphor was still giving off from the frame before.
   * <p>
   * A tube does not go dark the moment the beam leaves; it fades. That is why a sprite moving
   * across a real screen has a tail and why the flicker of a game that alternates frames is
   * softer there than here. Keeping a fraction of the last frame is the whole of it.
   */
  public static class Phosphor implements ScreenEffect {
    private final double persistence;

    public Phosphor(double persistence) {
      this.persistence = persistence;
    }

    public String label() {
      return "Phosphor persistence";
    }

    public boolean isTransparent() {
      return persistence <= 0;
    }

    public BufferedImage apply(BufferedImage picture, ScreenContext context) {
      BufferedImage before = context.buffer("phosphor", picture.getWidth(), picture.getHeight());
      int[] now = ScreenContext.pixelsOf(picture);
      int[] then = ScreenContext.pixelsOf(before);
      int old = (int) (persistence * 100), fresh = 100 - old;
      for (int i = 0; i < now.length; i++) {
        int a = now[i], b = then[i];
        // The brighter of the two, weighted: a phosphor fades, it does not average, so what was
        // lit stays lit a moment rather than pulling what is dark up towards it.
        int r = Math.max((a >> 16) & 0xFF, (((b >> 16) & 0xFF) * old + ((a >> 16) & 0xFF) * fresh) / 100);
        int g = Math.max((a >> 8) & 0xFF, (((b >> 8) & 0xFF) * old + ((a >> 8) & 0xFF) * fresh) / 100);
        int bl = Math.max(a & 0xFF, ((b & 0xFF) * old + (a & 0xFF) * fresh) / 100);
        int blended = 0xFF000000 | r << 16 | g << 8 | bl;
        now[i] = blended;
        then[i] = blended;
      }
      return picture;
    }
  }

  /** What the colours are turned into on the way out: for old eyes, dark rooms, or an old tube. */
  public static class Tint implements ScreenEffect {
    /** Which way the colours are pushed. */
    public enum Shade {
      NONE("None"), GREEN("Green phosphor"), AMBER("Amber phosphor"),
      GREY("Black and white"), SEPIA("Sepia"), WARM("Warm (less blue)");

      private final String label;

      Shade(String label) {
        this.label = label;
      }

      public String label() {
        return label;
      }
    }

    private final Shade shade;
    private final double brightness;
    private final double saturation;

    public Tint(Shade shade, double brightness, double saturation) {
      this.shade = shade;
      this.brightness = brightness;
      this.saturation = saturation;
    }

    public String label() {
      return "Colour";
    }

    public boolean isTransparent() {
      return shade == Shade.NONE && brightness == 1 && saturation == 1;
    }

    public BufferedImage apply(BufferedImage picture, ScreenContext context) {
      int[] pixels = ScreenContext.pixelsOf(picture);
      for (int i = 0; i < pixels.length; i++) {
        int rgb = pixels[i];
        double r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        double luma = 0.299 * r + 0.587 * g + 0.114 * b;
        switch (shade) {
          case GREEN -> { r = luma * 0.20; g = luma; b = luma * 0.25; }
          case AMBER -> { r = luma; g = luma * 0.75; b = luma * 0.10; }
          case GREY -> { r = luma; g = luma; b = luma; }
          case SEPIA -> { r = luma * 1.07; g = luma * 0.93; b = luma * 0.72; }
          case WARM -> b *= 0.80;
          case NONE -> { }
        }
        if (saturation != 1) {
          double mid = 0.299 * r + 0.587 * g + 0.114 * b;
          r = mid + (r - mid) * saturation;
          g = mid + (g - mid) * saturation;
          b = mid + (b - mid) * saturation;
        }
        pixels[i] = 0xFF000000
            | clamp(r * brightness) << 16 | clamp(g * brightness) << 8 | clamp(b * brightness);
      }
      return picture;
    }
  }

  private static int scaleRgb(int rgb, int percent) {
    return rgb & 0xFF000000
        | (((rgb >> 16) & 0xFF) * percent / 100) << 16
        | (((rgb >> 8) & 0xFF) * percent / 100) << 8
        | ((rgb & 0xFF) * percent / 100);
  }

  private static int clamp(double value) {
    return value <= 0 ? 0 : value >= 255 ? 255 : (int) (value + 0.5);
  }
}

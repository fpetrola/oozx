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

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The scalers, one class each.
 * <p>
 * WHAT IS HERE AND WHAT IS NOT. Everything here is written from the published rules of its
 * algorithm - Scale2x and Scale3x say plainly what they do in a dozen lines of conditions, and
 * sharp bilinear is a whole-number step followed by an ordinary resize. What is NOT here is hqx
 * and xBRZ: their reference implementations are GPL and LGPL, this project is Apache 2.0, and
 * copying them in would be a licence decision rather than a technical one. The edge-directed
 * scaler below is this project's own and does not claim to be either of them.
 */
public final class Scalers {

  private Scalers() {
  }

  /** Every scaler there is, in the order someone would meet them. */
  public static List<Scaler> all() {
    return List.of(new Nearest(), new Bilinear(), new SharpBilinear(),
        new Scale2x(), new Scale3x(), new EdgeDirected());
  }

  public static Scaler byName(String name) {
    for (Scaler scaler : all()) {
      if (scaler.getClass().getSimpleName().equalsIgnoreCase(name)
          || scaler.label().equalsIgnoreCase(name)) {
        return scaler;
      }
    }
    return new Nearest();
  }

  /**
   * Every pixel of the machine repeated as a block, and nothing invented.
   * <p>
   * At a whole-number scale this is exactly what the machine drew, magnified. At anything else
   * some blocks come out a pixel wider than their neighbours, which on a grid of eight-pixel
   * letters is visible as unevenness - the reason the other scalers exist.
   */
  public static class Nearest implements Scaler {
    public String label() {
      return "Nearest neighbour";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      BufferedImage out = context.buffer("scaled", width, height);
      Scaler.repeat(ScreenContext.readablePixels(picture), picture.getWidth(), picture.getHeight(),
          ScreenContext.pixelsOf(out), width, height);
      return out;
    }
  }

  /** Averaged between neighbours: even at any scale, and soft at every one of them. */
  public static class Bilinear implements Scaler {
    public String label() {
      return "Bilinear";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      return resize(picture, width, height, context, "scaled", true);
    }
  }

  /**
   * As many whole steps as fit, and the remainder averaged.
   * <p>
   * The trade the other two cannot make. Repeating pixels is exact but uneven at a fraction;
   * averaging is even but soft everywhere. Going up by whole pixels first leaves only the
   * fraction to average, so the edges stay where the machine put them and the unevenness goes.
   */
  public static class SharpBilinear implements Scaler {
    public String label() {
      return "Sharp bilinear";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      int steps = Math.max(1, Math.min(width / picture.getWidth(), height / picture.getHeight()));
      if (steps == 1) {
        return resize(picture, width, height, context, "scaled", true);
      }
      BufferedImage stepped = context.buffer("stepped",
          picture.getWidth() * steps, picture.getHeight() * steps);
      Scaler.repeat(ScreenContext.readablePixels(picture), picture.getWidth(), picture.getHeight(),
          ScreenContext.pixelsOf(stepped), stepped.getWidth(), stepped.getHeight());
      return resize(stepped, width, height, context, "scaled", true);
    }
  }

  /**
   * Scale2x, from AdvanceMAME: each pixel becomes four, and a corner takes a neighbour's colour
   * where two neighbours agree and the two across from them do not.
   * <p>
   * That one rule is the whole algorithm, and it is enough to round the staircase off a diagonal
   * without inventing a colour that was not on the screen - which is why it suits a machine whose
   * palette is fifteen colours and whose art was drawn a pixel at a time.
   */
  public static class Scale2x implements Scaler {
    public String label() {
      return "Scale2x";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      int sw = picture.getWidth(), sh = picture.getHeight();
      int[] source = ScreenContext.readablePixels(picture);
      BufferedImage doubled = context.buffer("doubled", sw * 2, sh * 2);
      int[] out = ScreenContext.pixelsOf(doubled);
      for (int y = 0; y < sh; y++) {
        for (int x = 0; x < sw; x++) {
          int p = source[y * sw + x];
          int a = Scaler.at(source, sw, sh, x, y - 1);
          int b = Scaler.at(source, sw, sh, x + 1, y);
          int c = Scaler.at(source, sw, sh, x - 1, y);
          int d = Scaler.at(source, sw, sh, x, y + 1);
          int at = y * 2 * sw * 2 + x * 2;
          out[at] = c == a && c != d && a != b ? a : p;
          out[at + 1] = a == b && a != c && b != d ? b : p;
          out[at + sw * 2] = d == c && d != b && c != a ? c : p;
          out[at + sw * 2 + 1] = b == d && b != a && d != c ? d : p;
        }
      }
      return fit(doubled, width, height, context);
    }
  }

  /** Scale3x, the same idea over nine pixels: rounder diagonals, and three times the size. */
  public static class Scale3x implements Scaler {
    public String label() {
      return "Scale3x";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      int sw = picture.getWidth(), sh = picture.getHeight();
      int[] s = ScreenContext.readablePixels(picture);
      BufferedImage tripled = context.buffer("tripled", sw * 3, sh * 3);
      int[] out = ScreenContext.pixelsOf(tripled);
      int tw = sw * 3;
      for (int y = 0; y < sh; y++) {
        for (int x = 0; x < sw; x++) {
          int a = Scaler.at(s, sw, sh, x - 1, y - 1), b = Scaler.at(s, sw, sh, x, y - 1);
          int c = Scaler.at(s, sw, sh, x + 1, y - 1), d = Scaler.at(s, sw, sh, x - 1, y);
          int e = s[y * sw + x], f = Scaler.at(s, sw, sh, x + 1, y);
          int g = Scaler.at(s, sw, sh, x - 1, y + 1), h = Scaler.at(s, sw, sh, x, y + 1);
          int i = Scaler.at(s, sw, sh, x + 1, y + 1);
          int at = y * 3 * tw + x * 3;
          out[at] = d == b && d != h && b != f ? d : e;
          out[at + 1] = (d == b && d != h && b != f && e != c)
              || (b == f && b != d && f != h && e != a) ? b : e;
          out[at + 2] = b == f && b != d && f != h ? f : e;
          out[at + tw] = (d == b && d != h && b != f && e != g)
              || (d == h && d != b && h != f && e != a) ? d : e;
          out[at + tw + 1] = e;
          out[at + tw + 2] = (b == f && b != d && f != h && e != i)
              || (f == h && d != h && b != f && e != c) ? f : e;
          out[at + tw * 2] = d == h && d != b && h != f ? d : e;
          out[at + tw * 2 + 1] = (f == h && d != h && b != f && e != g)
              || (d == h && d != b && h != f && e != i) ? h : e;
          out[at + tw * 2 + 2] = f == h && d != h && b != f ? f : e;
        }
      }
      return fit(tripled, width, height, context);
    }
  }

  /**
   * This project's own: doubles like Scale2x, and where a corner sits inside a diagonal run it
   * mixes the two colours rather than choosing one.
   * <p>
   * Scale2x will only ever put a colour that was already there, which keeps a picture honest and
   * leaves a diagonal a staircase of smaller steps. Blending the corner half and half turns those
   * steps into a slope. It is a smaller idea than what hqx and xBR do - they look at a wider
   * neighbourhood and weight the blend by how sure they are - and it is written here rather than
   * taken from them, whose implementations are GPL and LGPL against this project's Apache 2.0.
   */
  public static class EdgeDirected implements Scaler {
    public String label() {
      return "Edge directed";
    }

    public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
      int sw = picture.getWidth(), sh = picture.getHeight();
      int[] s = ScreenContext.readablePixels(picture);
      BufferedImage doubled = context.buffer("doubled", sw * 2, sh * 2);
      int[] out = ScreenContext.pixelsOf(doubled);
      int tw = sw * 2;
      for (int y = 0; y < sh; y++) {
        for (int x = 0; x < sw; x++) {
          int p = s[y * sw + x];
          int a = Scaler.at(s, sw, sh, x, y - 1), b = Scaler.at(s, sw, sh, x + 1, y);
          int c = Scaler.at(s, sw, sh, x - 1, y), d = Scaler.at(s, sw, sh, x, y + 1);
          int at = y * 2 * tw + x * 2;
          out[at] = c == a && c != d && a != b ? mix(a, p) : p;
          out[at + 1] = a == b && a != c && b != d ? mix(b, p) : p;
          out[at + tw] = d == c && d != b && c != a ? mix(c, p) : p;
          out[at + tw + 1] = b == d && b != a && d != c ? mix(d, p) : p;
        }
      }
      return fit(doubled, width, height, context);
    }

    private static int mix(int one, int other) {
      return (one & 0xFEFEFE) >> 1 & 0x7F7F7F | (other & 0xFEFEFE) >> 1 & 0x7F7F7F
          | ((one & other) & 0x010101);
    }
  }

  /**
   * The last step down to the window, after a scaler has taken its whole steps.
   * <p>
   * Repeating rather than averaging: what has been doubled or tripled is already the shape the
   * scaler decided on, and softening it here would undo the deciding.
   */
  private static BufferedImage fit(BufferedImage stepped, int width, int height,
                                   ScreenContext context) {
    if (stepped.getWidth() == width && stepped.getHeight() == height) {
      return stepped;
    }
    BufferedImage out = context.buffer("scaled", width, height);
    Scaler.repeat(ScreenContext.readablePixels(stepped), stepped.getWidth(), stepped.getHeight(),
        ScreenContext.pixelsOf(out), width, height);
    return out;
  }

  private static BufferedImage resize(BufferedImage picture, int width, int height,
                                      ScreenContext context, String name, boolean smooth) {
    BufferedImage out = context.buffer(name, width, height);
    java.awt.Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
        ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
        : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(picture, 0, 0, width, height, null);
    g.dispose();
    return out;
  }
}
